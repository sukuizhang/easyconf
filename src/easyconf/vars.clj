(ns easyconf.vars
  (:require [resource-monitor.core :as monitor])
  (:import [java.io File FileInputStream InputStreamReader BufferedReader]))

(def ^:private conf-vars (atom nil))

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
                                (and (not result) (m-var :validate-msg "validate fail !")))))]
    (validate-err var conf validate-msg)
    (.bindRoot var value)
    var))

(defn add-var!
  "add a var to be config"
  [var]
  (swap! conf-vars assoc
         (keyword (or (:conf-name (meta var)) (.sym var)))
         var))

(defn set-conf!
  "set config value"
  [conf]
  (if-let [var ((keyword (:conf-name conf)) @conf-vars)]
      (set-var var conf)))
