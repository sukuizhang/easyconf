(ns easyconf.load
  (:require [resource-monitor.core :as monitor]
            [easyconf.confs :as confs]
            [clojure.tools.logging :as logging])
  (:import [java.io File FileInputStream InputStreamReader BufferedReader]))

(defn parse-ns-name
  "parse ns name from clojure script, find (ns ...) mark to parse"
  [script]
  (let [pattern #"[(]ns\s+[a-zA-Z_-][a-zA-Z0-9_.-]*\s*"
        matches (re-seq pattern script)]
    (if (not (empty? matches)) 
      (-> matches
          (first)
          (.substring 3)
          (.trim)))))

(defn read-script
  "read clojure script from file, support *.clj file and *.properties file
   as you see below, we translate content in *.properties files to clojure script."
  [file-name]
  (cond  (.endsWith file-name ".clj")
         (slurp file-name)
         (.endsWith file-name ".properties")
         (with-open [in (-> file-name
                            (FileInputStream.)
                            (InputStreamReader.)
                            (BufferedReader.))]
           (let [lines (line-seq in)]
             (reduce (fn [buf line]
                       (let [line (.trim line)]
                         (or (and (or (empty? line) (= \# (.charAt line 0))) buf)
                             (let [idx (.indexOf line (int \=))
                                   var-name (.trim (.substring line 0 idx))
                                   var (.trim (.substring line (+ idx 1)))
                                   sep (if (not (empty? buf)) "\n")]
                               (str buf sep  "(def " var-name " \"" var "\")"))))) "" lines)))))

(def ^:private seek (atom 0))

(defn process-ns-script
  "for purpose that you can skip ns define, when this instance, we add a (ns ...) script to it.
   in all instance, we return the ns symbol and clojure script after processed."
  [script]
  (let [ns-name (parse-ns-name script)]
    (or (and ns-name
             [(symbol ns-name) script])
        (let [auto-ns-name (str "conf-namespace-auto" (swap! seek inc))]
          [(symbol auto-ns-name) (str "(ns " auto-ns-name ")\n" script)]))))

(defn load-script
  "load clojure script,and then return the corresponding namespace."
  [script]
  (let [[ns-sym script] (process-ns-script script)]
    (load-string script)
    (find-ns ns-sym)))

(comment "help confirmming that do once load only.")
(def ^:private loaded (atom false))

(defn load-conf-ns
  "load a namespace and add config item one by one."
  [file-name ns]
  (doseq [[var-sym var] (ns-publics ns)]
    (confs/add-conf-value file-name ns (name var-sym) var)))

(defn resources
  "search all valid config file in special config path."
  [path] 
  (->> (file-seq (File. path))
       (map #(.getAbsolutePath %1))
       (filter #(or (.endsWith %1 ".clj") (.endsWith %1 ".properties")))))

(defn loader
  "a loader used to load config items from special file."
  [file-name]
  (->> file-name
       read-script
       load-script
       (load-conf-ns file-name)))
  
(defn load-conf-path0
  "load config items from special config path, and monitor change on these files, reload config on change"
  [path resources loader]
  (or @loaded
      (swap! loaded 
             (fn [_]
               (doseq [resource (resources path)]
                 (logging/info (str "install config resource:" path))
                 (monitor/monitor resource resource {:visit-file [loader]}))
               true))))

(defn load-conf-path
  ([] (let [path (or (System/getProperty "conf-path") "etc")]
        (load-conf-path path)))
  ([path] (load-conf-path0 path resources loader)))
