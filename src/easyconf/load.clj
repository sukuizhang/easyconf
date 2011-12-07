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

(defn load-script [script]
  (let [p-fn (fn [script]
               (let [ns-name (parse-ns-name script)]
                 (or (and ns-name
                          [(symbol ns-name) script])
                     (let [auto-ns-name (str "conf-namespace-auto" (swap! seek inc))]
                       [(symbol auto-ns-name (str "(ns " auto-ns-name ")\n" script))]))))
        [ns-sym script] (p-fn script)]
    (load-string script)
    (find-ns ns-sym)))

(def ^:private confs (atom nil))

(defn add-conf-value [file-name ns conf-name value]
  (let [conf {:file-name file-name :ns ns :conf-name conf-name :value (var-get value)}]
    (swap! confs assoc (keyword conf-name) conf)
    (vars/set-conf! conf)))

(defn load-conf-ns
  [file-name ns]
  (let [ns (find-ns ns)]
    (doseq [[var-sym var] (ns-publics ns)]
      (add-conf-value file-name ns (name var-sym) var))))

(defn load-conf-path
  [path]
  (let [resources (->> (file-seq (File. path))
                       (map #(.getAbsolutePath %1))
                       (filter #(or (.endsWith %1 ".clj") (.endsWith %1 ".properties"))))
        loader (fn [file-name] (->> file-name
                                   read-script
                                   load-script
                                   (load-conf-ns file-name)))]
    (doseq [resource resources]
      (monitor/monitor resource resource {:visit-file [loader]}))))
