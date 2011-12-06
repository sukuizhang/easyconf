(ns easyconf.load
  (:require resource-monitor.core :as monitor)
  (:import [java.io File FileInputStream InputStreamReader BufferedReader]))

(def ^:private conf-vars (atom nil))
(def ^:private conf-vals (atom nil))

(defn parse-ns-name [script]
  (let [pattern #"[(]ns\s+[a-zA-Z_-][a-zA-Z0-9_.-]*\s*"
        matches (re-seq pattern script)]
    (if (not (empty? matches)) 
      (-> matches
          (first)
          (.substring 3)
          (.trim)))))

(def ^:private seek (atom 0))

(defn- conf-val-changed [val conf-vars]
  )

(defn load-conf-val [ns var-name var]
  )


(defn load-conf-ns
  [ns]
  (let [ns (find-ns ns)]
    (doseq [[var-sym var] (ns-publics ns)]
      (load-conf-val ns var-sym var))))

(defn load-script [script]
  (let [p-fn (fn [script]
               (let [ns-name (parse-ns-name script)]
                 (or (and ns-name
                          [(symbol ns-name) script])
                     (let [auto-ns-name (str "conf-namespace-auto" (swap! seek inc))]
                       [(symbol auto-ns-name (str "(ns " auto-ns-name ")\n" script))]))))
        [ns-sym script] (p-fn script)]
    (load-string script)
    (load-conf-ns (find-ns ns-sym))))


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
                       (println line)
                       (let [line (.trim line)]
                         (or (and (or (empty? line) (= \# (.charAt line 0))) buf)
                             (let [idx (.indexOf line (int \=))
                                   var-name (.trim (.substring line 0 idx))
                                   var (.trim (.substring line (+ idx 1)))
                                   sep (if (not (empty? buf)) "\n")]
                               (str buf sep  "(def " var-name " \"" var "\")"))))) "" lines)))))

(defn load-conf-path
  [path]
  (let [resources (->> (file-seq (File. path))
                       (map #(.getAbsolutePath %1))
                       (filter #(or (.endsWith %1 ".clj") (.endsWith %1 ".properties"))))
        loader (fn [file-name] (-> file-name read-script load-script))]
    (doseq [resource resources]
      (monitor/monitor resource resource {:visit-all [loader]}))))

(defn- explain-var-name [var-name]
  (let [pos (.lastIndexOf var-name ".")]
    (if (= -1 pos)
      [nil var-name]
      [(.substring var-name 0 pos) (.substring var-name (+ 1 pos))])))

(defn decide-to-ns
  [ns to-ns to-var-name]
  (or (and to-ns (find-ns (symbol to-ns)))
      (if-let [ns-list (map #(find-ns %) (var-get ((ns-publics ns) 'ns-list)))]
        (first (filter #((ns-map %) (symbol to-var-name)) ns-list)))))

(defn inner-load [ns var-name var]
  (let [[to-ns to-var-name] (explain-var-name var-name)
        to-ns (decide-to-ns ns to-ns to-var-name)]
    (if to-ns
      (if-let [init-var ((ns-map to-ns) (symbol var-name))]
        (let [validator (:validator (meta init-var))]
          (if (and validator (not (validator var)))
            (throw (Exception.
                    (str "error load conf: "
                         (str ns) "/" var-name
                         " validate fail for \n" validator
                         "\nmsg: " (:validate-msg (meta init-var)))))
            (intern to-ns (symbol to-var-name) var)))))))

(def load-conf-ns! (partial load-conf-ns inner-load))
(def load-conf-path! (partial load-conf-path inner-load))


