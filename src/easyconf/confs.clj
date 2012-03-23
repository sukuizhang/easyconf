(ns easyconf.confs
  (:require [resource-monitor.core :as monitor]
            [clojure.set :as set]
            [clojure.tools.logging :as logging])
  (:import [java.io File FileInputStream InputStreamReader BufferedReader]))

(comment "all config vars, normally it add in when compile source file")
(def ^:private conf-vars (atom nil))
(comment
  "all config values, it load from config path when first use, auto reload when config file has changed.
   when config value first load or changed, it will inject to according config var.")
(def ^:private conf-vals (atom nil))
(comment
  "when config value of :no-useless-config has set, no useless config item check option is open
   when config value of :all-var-must-config has set, all var must be config check option is open")
(defonce ^:private check-options [:no-useless-config :all-var-must-config])

(defn validate-err
  "throw a Exception when validate fail."
  [var conf msg]
  (if msg
    (let [{:keys [file-name ns conf-name value]} conf]    
      (throw (Exception.
              (str "error load conf [file name:" file-name
                   " conf name:" conf-name
                   " value:" value "]"    
                   "\nvalidate fail for " var
                   "\nmsg: " msg))))))

(defn set-var
  "change root binding of the var, a validate will be take if it announce on the var use :validator meta, and you can assign validate fail message.
 when your config value is string and var type is not, it load this config string to clojure data and use it as new value. var type from :tag meta or the initial var value, if not, it guess it as string"
  [var conf]
  (logging/info (str "inject config value:" conf " to var:" var))
  (let [m-var (meta var)
        require-type (or (m-var :tag (type (var-get var)))
                         String)
        value (:value conf)
        value (if (and (not= String require-type)
                       (= String (type value)))
                (load-string value)
                value)
        validator (:validator m-var)
        validate-msg (and validator
                          (let [result (validator value)]
                            (or (and (string? result) result)
                                (and (not (nil? result)) (not result) (m-var :validate-msg "validate fail !")))))]
    (validate-err var conf validate-msg)
    (.bindRoot var value)
    var))

(defn add-var!
  "add a var to be config"
  [var]
  (let [key (keyword (or (:conf-name (meta var)) (.sym var)))
        conf (key @conf-vals)]
    (if conf
      (logging/info (str "add config var:" var " inject value from:" conf))
      (logging/info (str "add config var:" var " no config value found !")))
    (swap! conf-vars assoc key var)
    (when (not= clojure.lang.Var$Unbound (type (var-get var)))
      (if conf
        (logging/info (str "default config value:" (var-get var) " be overrided with config item" conf))
        (do
          (logging/info (str "no config value found, use default config value:" (var-get var)))
          (swap! conf-vals assoc key (var-get var)))))
    (if conf (set-var var conf))))

(defn add-conf-value
  "add a config value"
  [file-name ns conf-name value]
  (let [key (keyword conf-name)
        var (key @conf-vars)
        conf {:file-name file-name :ns ns :conf-name conf-name :value (var-get value)}]
    (logging/info (str "add config item:" conf " inject to var:" var))
    (swap! conf-vals assoc key conf)
    (if var (set-var var conf))))

(defn check
  "check if config items is according to define config vars"
  []
  (let [errors {:useless (and ((first check-options) @conf-vals)
                              (set/difference
                               (set (keys @conf-vals))
                               (set (keys @conf-vars))))
                :config-not-found (and ((first check-options) @conf-vals)
                                       (set/difference
                                        (set (keys @conf-vars))
                                        (set (keys @conf-vals))))}
        not-option (fn [key] (not-any? #(= key %) check-options))
        error-msgs (-> errors
                       (assoc-in [:useless] (->> (:useless errors)
                                              (filter not-option)
                                              (map (fn [key] (str "useless config item:" (key @conf-vals))))))
                       (assoc-in [:config-not-found] (->> (:config-not-found errors)
                                                     (filter not-option)
                                                     (map (fn [key] (str "not config var:" (key @conf-vars)))))))
        msg (->> (or (and (< 0 (count (:config-not-found error-msgs)))
                  (apply conj (:useless error-msgs) (:config-not-found error-msgs)))
             (:useless error-msgs))
         (interpose "\n")
         (apply str)
         (#(if (< 0 (count %)) (str "check error msgs:\n" %))))]
    (if msg
      (throw (Exception. msg)))))
