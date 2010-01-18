
(ns user (:use tokyo-cabinet))  ;; bring into our namespace

(with-cabinet { :filename "test.tokyo" :mode (+ OWRITER OCREAT) } 
  (doseq [[name val] [["1" "one"] ["2" "two"] ["3" "three"]]]
    (put-value name val)))

(with-cabinet { :filename "test.tokyo" :mode OREADER } 
  (println (get-value "1")))


;; tables

(def params { :filename "test-table.tokyo" :mode (+ OWRITER OCREAT) :type :table } )
(with-cabinet params
  (put-value nil { :name "John Doe" :hobbies "rowing fishing skiing" :age 28 :gender "M" })
  (put-value nil { :name "Melissa Swift" :hobbies "soccer tennis books" :age 33 :gender "F"})
  (put-value nil { :name "Tom Swift" :hobbies "inventing exploring" :gender "M" })
  (put-value nil { :name "Harry Potter" :hobbies "magic quidditch flying" :gender "M" :age 9 }))



