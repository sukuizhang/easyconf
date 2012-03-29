(ns easyconf.confs
  (:require [clojure.set :as set]
            [clojure.tools.logging :as logging]))


;;every conf-name correspends to a unique clojure var
;;when the value of a speify conf-name changes, it will automaticly change
;;the value of the var

;;{conf-name1 conf-value1 conf-name2 conf-value2}
(def ^:private conf-vals (atom {}))
;;{conf-name1 conf-var1 conf-name2 conf-var2}
(def ^:private conf-vars (atom {}))

(defn reset-conf-vals
  "clear all config values"
  []
  (reset! conf-vals (atom {})))

(defn get-conf-name
  [var]
  (keyword (or (:conf-name (meta var)) (.sym var))))

(defn validate-err
  "throw an Exception when validate fail."
  [var f value msg]
  (when f
    (when-not (f value) 
      (throw (RuntimeException.
              (str "error load config , conf-name " (get-conf-name var)
                   " value:" value     
                   "to " var
                   "\nmsg: " msg))))))

(defn set-var-value
  "change root binding of the var, a validate will be take if it announce on the var use :validator meta, and you can assign validate fail message."
  [var value]
  (logging/info (str "inject config value:" value " to var:" var))
  (let [m-var (meta var)
        validator (:validator m-var)
        validate-msg (:validator-msg m-var)]
    (validate-err var validator value validate-msg)
    (.bindRoot var value)
    var))

(defn config-only-once? []
  (System/getProperty "conf.onlyonce"))

(defn set-config-only-once! [on]
  (if on
    (System/setProperty "conf.onlyonce" "true")
    (.remove (System/getProperties) "conf.onlyonce")))

(defn config-value
  "change the var value"
  [conf-name value]
  (swap! conf-vals assoc conf-name value)
  (when (find @conf-vars conf-name)
    (set-var-value (conf-name @conf-vars) value)))

(defn add-var!
  "add a var to be configured"
  [var]
  (let [conf-name (get-conf-name var)
        unbound (= clojure.lang.Var$Unbound (type (var-get var)))]
    (swap! conf-vars assoc conf-name var)
    (if (find @conf-vals conf-name)
      (set-var-value var (conf-name @conf-vals))
      (if unbound
        (throw (RuntimeException. (str "var: " var " can not find config value")))
        (logging/info "var " var
                      " cannot find config value, using default: "
                      (var-get var))))))

(defn check
  "check if all config values are used"
  []
  (let [vals-keys (set (keys @conf-vals))
        vars-keys (set (keys @conf-vars))
        diff (set/difference vals-keys vars-keys)]
    (when-not (seq diff)
      (throw (RuntimeException. (str "config " diff " haven't be used" ))))))

(defn default-conf-path []
  (or (System/getProperty "conf.path")
      "etc"))

(defn load-conf-path
  "load clojure script from special path"
  [path]
  (let [s-f (java.io.File. path)]
    (if (.isFile s-f)
      (when (.endsWith path ".clj")
        (do
          (load-string "use 'easyconf.core")
          (load-file path)))
      (do
        (doseq [sub-file (.list s-f)]
            (load-dir
              (str path (java.io.File/separator) sub-file)))))))

(defn monitor-conf-path
  [path]
  )
