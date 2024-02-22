(ns create-new-file
  (:require [picoautomator.core :refer :all]
            [common]))

(defn -main
  [& args]

  (start-flow
    "Should create new file"
    (fn [automator]
      (launch automator "com.ivanovsky.passnotes")

      (when (visible? automator {:text "content://fakefs.com/new-file.kdbx"})
        (-> automator
            (long-tap-on {:text "content://fakefs.com/new-file.kdbx"})
            (tap-on {:text "Remove"})))

      (-> automator
          (common/open-new-database-screen)
          (tap-on {:text "Storage type:"})
          (tap-on {:text "Fake File System"})
          (assert-visible (list {:text "content://fakefs.com"} {:text "user"}))
          (tap-on {:content-desc "Done"})
          (assert-visible {:text "Select directory"})
          (tap-on {:content-desc "Done"})
          (assert-visible {:text "New database"})
          (tap-on {:text "File name"})
          (input-text "new-file")
          (tap-on {:text "Password"})
          (input-text "abc123")
          (tap-on {:text "Confirm"})
          (input-text "abc123")
          (tap-on {:content-desc "Done"})
          (wait-until {:text "Database"} {:seconds 5} {:seconds 1})))))
