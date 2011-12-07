(ns easyconf.test.load
  (:use [easyconf.load]
        [midje.sweet])
  (:import [java.io File]))

(fact "parse ns name"
  (parse-ns-name "xla\nsd\n(ns ab.cd-ef.xy\n:import)") => "ab.cd-ef.xy"
  (parse-ns-name "xla\nsd\n(ns ab.cd_ef12.xy :import)") => "ab.cd_ef12.xy"
  (parse-ns-name "xla\nsd\n(ns ab.cd.xy)") => "ab.cd.xy"
  (parse-ns-name "(def ab 3)") => nil
  (parse-ns-name "ab = 7") => nil)

(fact "read script from file"
  (let [content {"test/temp" ["xxxx" nil]
                 "test/temp.pp" ["xxxx" nil]
                 "test/temp.clj" ["(def a 1)" "(def a 1)"]
                 "test/temp.properties"
                 ["#\na = 3\nb=7\n\n#heihei=7\nx= [1 2] "
                   "(def a \"3\")\n(def b \"7\")\n(def x \"[1 2]\")"]}]
    (doseq [[f [c v]] content]
      (spit f c)
      (read-script f) => v
      (.delete (File. f)))))

(fact "process ns script"
  (process-ns-script "(ns abc.efg)\nxyz") => ['abc.efg "(ns abc.efg)\nxyz"]
  (doseq [script0 ["(def a 1)" "(def b 1)\n(def c 1))"]]
    (let [[ns script] (process-ns-script script0)]
      (symbol (parse-ns-name script)) => ns)))
