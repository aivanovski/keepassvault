(ns add-templates-should-work
  (:require [picoautomator.core :refer :all]
            [common]))

(defn flow
  [automator]

  (-> automator
      (launch "com.ivanovsky.passnotes")
      (common/unlock-db "automation.kdbx")
      (assert-visible {:text "Database"})
      (assert-not-visible {:text "Templates"})
      (tap-on {:content-desc "More options"})
      (tap-on {:text "Add note templates"})
      (tap-on {:text "YES"})
      (wait-until {:text "Templates"} {:seconds 10} {:seconds 1})
      (tap-on {:text "Templates"})
      (assert-visible
        (list
          {:text "Credit card"}
          {:text "E-Mail"}
          {:text "ID card"}
          {:text "Membership"}
          {:text "Secure note"}
          {:text "Wireless LAN"}))))

(defn -main
  [& args]

  (start-flow
    "Add templates should add group with entries"
    flow))
