easyconf

a very easy config tool.

Usage
1.add dependency [easyconf "0.0.1"] in your project.clj

2.add use easyconf.core to your source unit, it provide two macro defconf and defnconf, you can use them just like def and defn.

3.when you like to make a config var, please use defconf or defnconf, instead of def or defn, like below:
       (defconf max-coin-per-day 5000)
after this, you can use it just as 
      (def max-coin-per-day 5000)
there is no difference if you do nothing else.

4.put your config file into config path.
  default config path is "etc", you can use jvm option -Dconf-path= to specify others. you can put your config file anywhere in it.
  your config file has two format:
  (a)clojure clj file:
     now wirte
     (def max-coin-per-day 50)
     to your xyz.clj,and put it into config path, and then you will find your config var max-coin-per-day use in anywhere has change to 50.
  (b)properties file:
    and you also can work with properties file.
    if you write
    max-coin-per-day=50
    to your xyz.properties, and put it into config path, exactly the same,you find your config var has change to 50.
    
   you can place many config files in your path.

5.validate your config value.
  just add :validator meta to your config var is ok, and you can add :validate-msg meta to specify error msg that will show as complie error message.
  your validator can return boolean value (true is ok), or return string value (nil is ok).

6.specify config name.
 default, we use var symbol name to match config item, but some time, you will meet same var symbol name, in this instance, you can use :conf-name meta to specify other config name, for example: 
         (defconf ^{:conf-name "max-coin-per-day"} max-coin 5000)
then preview config would work.

7.monitor config file change.
  anytime when you modify your config file, your config var will auto change without restart your system.for example, if you change your xyz.properties to:
   max-coin-per-day=50000
and then save it.
  now in your running logic, this var has auto change to 50000, you do not need to restart your system. 

8.check your config items.
  If you has many many of config var in your project, maybe you will be worry about if you has omitted some of it, or you has write wrong config name.
  We has two config check option, if you open it, you can find these error in complie time. They are 'no-useless-config' and 'all-var-must-config', write:
no-useless-config=1
all-var-must-config=1
in any of your properties file, or 
(def no-useless-config 1)
(def no-useless-config 1)
in any of your clj file.
you can open these check options.
after you do this, you can invoke (easyconf.confs/check) in your main.clj, when all of your config var has load in. If you has write some config error, you will meet a complie error, and show you error detail.
