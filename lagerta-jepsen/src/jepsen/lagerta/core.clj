;;
;; Licensed to the Apache Software Foundation (ASF) under one or more
;; contributor license agreements.  See the NOTICE file distributed with
;; this work for additional information regarding copyright ownership.
;; The ASF licenses this file to You under the Apache License, Version 2.0
;; (the "License"); you may not use this file except in compliance with
;; the License.  You may obtain a copy of the License at
;;
;;      http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;

(ns jepsen.lagerta.core
  (:gen-class)
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [verschlimmbesserung.core :as v]
            [slingshot.slingshot :refer [try+]]
            [knossos.model :as model]
            [jepsen
             [checker :as checker]
             [cli :as cli]
             [client :as client]
             [control :as c]
             [db :as db]
             [generator :as gen]
             [tests :as tests]
             [util :as util :refer [timeout]]]
            [jepsen.checker.timeline :as timeline]
            [jepsen.control.util :as cu]
            [jepsen.os.debian :as debian]))


(def dir     "/opt/etcd")
(def binary  "etcd")
(def logfile (str dir "/etcd.log"))
(def pidfile (str dir "/etcd.pid"))



(defn node-url
  "An HTTP url for connecting to a node on a particular port."
  [node port]
  (str "http://" (name node) ":" port))

(defn peer-url
  "The HTTP url for other peers to talk to a node."
  [node]
  (node-url node 2380))

(defn client-url
  "The HTTP url clients use to talk to a node."
  [node]
  (node-url node 2379))

(defn initial-cluster
  "Constructs an initial cluster string for a test, like
  \"foo=foo:2380,bar=bar:2380,...\""
  [test]
  (->> (:nodes test)
       (map (fn [node]
              (str (name node) "=" (peer-url node))))
       (str/join ",")))

(defn parse-long
  "Parses a string to a Long. Passes through `nil`."
  [s]
  (when s (Long/parseLong s)))



(defn etcd-control
  "Etcd DB for a particular version."
  [version]
  (reify db/DB
    (setup! [_ test node]
      (info node "installing etcd" version)
      (c/su
        (let [url (str "https://storage.googleapis.com/etcd/" version
                       "/etcd-" version "-linux-amd64.tar.gz")]
          (cu/install-tarball! c/*host* url dir))

        (cu/start-daemon!
          {:logfile logfile
           :pidfile pidfile
           :chdir   dir}
          binary
          :--name                         (name node)
          :--listen-peer-urls             (peer-url   node)
          :--listen-client-urls           (client-url node)
          :--advertise-client-urls        (client-url node)
          :--initial-cluster-state        :new
          :--initial-advertise-peer-urls  (peer-url node)
          :--initial-cluster              (initial-cluster test))

          (Thread/sleep 10000)))

    (teardown! [_ test node]
      (info node "tearing down etcd")
      (cu/stop-daemon! binary pidfile)
      (c/su
        (c/exec :rm :-rf dir)))

    db/LogFiles
    (log-files [_ test node]
      [logfile])))

(defn client
  "A client for a single compare-and-set register"
  [conn]
  (reify client/Client
    (setup! [_ test node]
      (client (v/connect (client-url node)
                         {:timeout 5000})))

    (invoke! [this test op]
      (case (:f op)
        :read (assoc op :type :ok, :value (parse-long (v/get conn "r")))
        :write (do (v/reset! conn "r" (:value op))
                   (assoc op :type, :ok))
        :cas (try+
               (let [[value value'] (:value op)]
                 (assoc op :type (if (v/cas! conn "r" value value'
                                             {:prev-exist? true})
                                   :ok
                                   :fail)))
               (catch [:errorCode 100] _
                 (assoc op :type :fail, :error :not-found)))))

    (teardown! [_ test]
      ; If our connection were stateful, we'd close it here.
      ; Verschlimmbesserung doesn't hold a connection open, so we don't need to
      ; close it.
      )))

(defn r   [_ _] {:type :invoke, :f :read, :value nil})
(defn w   [_ _] {:type :invoke, :f :write, :value (rand-int 5)})
(defn cas [_ _] {:type :invoke, :f :cas, :value [(rand-int 5) (rand-int 5)]})

(defn etcd-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh, :concurrency, ...), constructs a test map."
  [opts]  
  (merge tests/noop-test
         {:name "etcd"
          :os debian/os
          :db (etcd-control "v3.1.5")
          :client (client nil)
          :model  (model/cas-register)
          :checker (checker/compose
                     {:perf   (checker/perf)
                      :timeline (timeline/html)
                      :linear checker/linearizable})
          :generator (->> (gen/mix [r w cas])
                          (gen/stagger 1)
                          (gen/clients)
                          (gen/time-limit 15))}
         opts))			

	  
(defn -main
  "Handles command line arguments. Can either run a test, or a web server for browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn etcd-test})
                   (cli/serve-cmd))
            args))