(ns unlock
  (:require [picoautomator.core :refer :all]
            [clojure.string :as str]))

(defn -main
  [& args]

  (start-flow
    "Unlock database"
    (fn [automator]
      (-> automator
          (launch "com.ivanovsky.passnotes")
          (assert-visible {:text "content://fakefs.com/automation.kdbx"})
          (tap-on {:text "content://fakefs.com/automation.kdbx"})
          (tap-on {:text "Password"})
          (input-text "abc123")
          (tap-on {:id "unlockButton"})
          (assert-visible {:text "Database"})))))
