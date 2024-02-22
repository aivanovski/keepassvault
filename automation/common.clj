(ns common
  (:require [picoautomator.core :refer :all]))

(defn open-new-database-screen
  [automator]

  (-> automator
      (assert-visible {:text "KPassNotes"})
      (tap-on {:id "fab"})
      (tap-on {:text "New file"})
      (assert-visible {:text "Storage type:"}))

  automator)

(defn unlock-db
  [automator db-name]
  (let [element-name (str "content://fakefs.com/" db-name)]
    (-> automator
        (assert-visible {:text element-name})
        (tap-on {:text element-name})
        (tap-on {:text "Password"})
        (input-text "abc123")
        (tap-on {:id "unlockButton"})
        (assert-visible {:text "Database"}))))
