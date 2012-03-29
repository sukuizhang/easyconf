(ns easyconf.confs
  (:require [clojure.set :as set]
            [clojure.tools.logging :as logging]
            [clojure.tools.namespace :as namespace]))


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
  (let [meta-var (meta var)]
    (keyword (or (:conf-name meta-var)
                 (:name meta-var)))))

(defn validate-err
  "throw a Exception when validate fail."
  [var f value msg]
  (when (and f (not (f value))) 
    (throw (RuntimeException.
            (str "error load config , conf-name " (get-conf-name var)
                 " value: " value     
                 " to " var
                 "\nmsg: " msg)))))

(defn if-conf-defined
  "judge whether the conf-name is defined"
  [conf-name]
  (find @conf-vals conf-name))

(defn set-var-value
  "change root binding of the var, a validate will be take if it announce on the var use :validator meta, and you can assign validate fail message."
  [var value]
  (logging/info (str "inject config value:" value " to var:" var))
  (let [m-var (meta var)
        validator (:validator m-var)
        validate-msg (:validator-msg m-var)]
    (validate-err var validator value validate-msg)
    (alter-var-root var (constantly value))
    var))

(defn config-value
  "change the var value"
  ^{:pre [(keyword? conf-name)]}
  [conf-name value]
  (swap! conf-vals assoc conf-name value)
  (when (find @conf-vars conf-name)
    (set-var-value (conf-name @conf-vars) value)))

(defn load-ns
  "Require all the namespaces prefixed by the namespace symbol given so that the dict and conf are loaded."
  [& ns-syms]
  (doseq [ns-sym ns-syms
          n (namespace/find-namespaces-on-classpath)
          :let [pattern (re-pattern (name ns-sym))]
          :when (re-seq pattern (name n))]
    (require n)))

(defn add-var!
  "add a var to be configured"
  [var]
  (let [conf-name (get-conf-name var)
        unbound (= clojure.lang.Var$Unbound (type (var-get var)))
        defined (if-conf-defined conf-name)]
    (swap! conf-vars assoc conf-name var)
    (if unbound
      (if-not defined
        (throw (RuntimeException. (str "var: " var " can not find config value")))
        (logging/info "var " var
                      " cannot find config value , using default: "
                      (var-get var)))
      (when defined
        (set-var-value var (conf-name @conf-vals))))))

(defn check
  "check if all config values are used"
  []
  (let [vals-keys (set (keys @conf-vals))
        vars-keys (set (keys @conf-vars))
        diff (set/difference vals-keys vars-keys)]
    (when-not (seq diff)
      (throw (RuntimeException. (str "config " diff " haven't be used" ))))))

(defn config-var-script
  [var]
  (-> (:comment (meta var))
      (#(if % (str ";" % "\n")))
      (str "(config-once " (get-conf-name var) " " (pr-str (var-get var)) ")\n")))

(defn config-script
  "create a config template file."
  [& [path]]
  (let [ns-file (str (or path "test") "/config/autocreate.clj")
        head "(ns config.autocreate\n  (:use    [easyconf.core]))\n\n"
        script (->> @conf-vars
                    vals
                    (map config-var-script)
                    (cons head)
                    (apply str))]
    (spit ns-file script)))
