(ns easyconf.load
  (:require [resource-monitor.core :as monitor]
            [easyconf.vars :as vars])
  (:import [java.io File FileInputStream InputStreamReader BufferedReader]))

(defn parse-ns-name [script]
  (let [pattern #"[(]ns\s+[a-zA-Z_-][a-zA-Z0-9_.-]*\s*"
        matches (re-seq pattern script)]
    (if (not (empty? matches)) 
      (-> matches
          (first)
          (.substring 3)
          (.trim)))))

(defn read-script
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

(defn process-ns-script [script]
  (let [ns-name (parse-ns-name script)]
    (or (and ns-name
             [(symbol ns-name) script])
        (let [auto-ns-name (str "conf-namespace-auto" (swap! seek inc))]
          [(symbol auto-ns-name) (str "(ns " auto-ns-name ")\n" script)]))))

(defn load-script [script]
  (let [[ns-sym script] (process-ns-script script)]
    (load-string script)
    (find-ns ns-sym)))

(def ^:private confs (atom nil))
(def ^:private loaded (atom false))
(defonce ^:private no-useless-config :no-useless-config)
(defonce ^:private all-var-has-config :all-var-has-config)

(defn add-conf-value [file-name ns conf-name value]
  (let [conf {:file-name file-name :ns ns :conf-name conf-name :value (var-get value)}]
    (swap! confs assoc (keyword conf-name) conf)
    (vars/set-conf! conf)))

(defn load-conf-ns
  [file-name ns]
  (let [ns (find-ns ns)]
    (doseq [[var-sym var] (ns-publics ns)]
      (add-conf-value file-name ns (name var-sym) var))))

(defn resources
  [path] 
  (->> (file-seq (File. path))
       (map #(.getAbsolutePath %1))
       (filter #(or (.endsWith %1 ".clj") (.endsWith %1 ".properties")))))

(defn loader
  [file-name]
  (->> file-name
       read-script
       load-script
       (load-conf-ns file-name)))
  
(defn load-conf-path0
  [path resources loader]
  (or @loaded
      (swap! loaded 
             (fn [_]
               (doseq [resource resources]
                 (monitor/monitor resource resource {:visit-file [loader]}))
               true))))

(defn load-conf-path [path] (load-conf-path0 path resources loader))
(defn check-no-useless-config? [] (:no-useless-config @confs))
(defn check-all-var-has-config? [] (:all-var-has-config @confs))
(defn all-confs [] @confs)
