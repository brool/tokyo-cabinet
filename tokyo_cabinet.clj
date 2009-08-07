;;
;; Clojure interface for Tokyo Cabinet.
;;
;; Copyright (c) 2009 by Tim Lopez. Licensed under Eclipse Public License 1.0.
;; 

(ns 
    #^{:author "Tim Lopez" 
       :doc "A Clojure interface to Tokyo Cabinet."}
  tokyo-cabinet)

(declare *tokyo*)                      ;; current cabinet
(declare *query*)                      ;; current table query (if any)

;;
;; use the cabinet
;;

(def OCREAT tokyocabinet.TDB/OCREAT)
(def OREADER tokyocabinet.TDB/OREADER)
(def OWRITER tokyocabinet.TDB/OWRITER)
(def OTRUNC tokyocabinet.TDB/OTRUNC)

(defn open-cabinet 
  "Open a Tokyo cabinet.  Is given a hashmap of connection parameters,
   of which only :filename is required. :type specifies the type of
   cabinet (legal values are :hash, :table, :fixed and :bplus, with
   the default being :hash).  :mode specifies the :open mode, and can
   be any of the flags OCREAT, OREADER, OWRITER, and OTRUNC. Usually
   you'll use with-cabinet, but if you use this you'll also need to
   use with and close-cabinet."
  [options]
  (let [fn (options :filename)
        cls-list { :hash "HDB" :table "TDB" :fixed "FDB" :bplus "BDB" }
        tokyo-type (options :type :hash)
        tokyo-mode (options :mode OREADER)
        cls-name (str "tokyocabinet." (cls-list tokyo-type))
        db (.newInstance (. Class forName cls-name))]
    (.open db fn tokyo-mode)
    { :connection db :type tokyo-type :mode tokyo-mode }
))

(defn close-cabinet
  "Closes a Tokyo cabinet."
  [cabinet]
  (.close (cabinet :connection)))

(defmacro with-cabinet 
  "Opens a cabinet and makes it available for the scope of body."
  [ options & body ]
  `(binding [*tokyo* (open-cabinet ~options)]
     (with-open [db# (*tokyo* :connection)]
       (do ~@body))))

(defmacro with [cabinet & body]
  `(binding [*tokyo* ~cabinet]
     (do ~@body)))
     

(defn- check-type 
  "Utility function.  Make sure that the cabinet is the right type."
  [type]
  (if (not (= type (*tokyo* :type)))
    (throw (IllegalArgumentException. (format "Expected cabinet type of %s but had %s" type (*tokyo* :type))))))

(defn genuid "Get a unique primary key for a table." []
  (check-type :table)
  (.genuid (*tokyo* :connection)))

(defn get-value "Gets a value from the cabinet." [key] (.get (*tokyo* :connection) (str key)))
(defn put-value 
  "Puts a value into the cabinet. With tables, you can use nil for the
  key to use an arbitrary unique key."  
  [key val]
  (when (nil? key) (check-type :table))
  (let [putkey (if (nil? key) (genuid) key)]
    (.put (*tokyo* :connection) (str putkey) val)))

;;
;; indexes
;;

(def INDEX-DECIMAL tokyocabinet.TDB/ITDECIMAL)
(def INDEX-KEEP tokyocabinet.TDB/ITKEEP)
(def INDEX-LEXICAL tokyocabinet.TDB/ITLEXICAL)
(def INDEX-OPTIMIZE tokyocabinet.TDB/ITOPT)
(def INDEX-QGRAM tokyocabinet.TDB/ITQGRAM)
(def INDEX-TOKEN tokyocabinet.TDB/ITTOKEN)
(def INDEX-VOID tokyocabinet.TDB/ITVOID)

(defn create-index 
  "Creates an index on the cabinet.  Legal index types are IT-DECIMAL, 
   INDEX-LEXICAL, INDEX-QGRAM and INDEX-TOKEN.  Setting INDEX-KEEP will
   keep the index if already exists, while INDEX-OPTIMIZE will optimize
   it.  Use either delete-index or INDEX-VOID to delete and index."
  [column type]
  (check-type :table)
  (.setindex (*tokyo* :connection) (str column) type))

(defn delete-index 
  "Deletes the given index from the cabinet."
  [column]
  (check-type :table)
  (.setindex (*tokyo* :connection) (str column) INDEX-VOID))

;; 
;; queries
;;

(def NUM-EQ tokyocabinet.TDBQRY/QCNUMEQ)
(def NUM-GT tokyocabinet.TDBQRY/QCNUMGT)
(def NUM-GE tokyocabinet.TDBQRY/QCNUMGE)
(def NUM-LE tokyocabinet.TDBQRY/QCNUMLE)
(def NUM-LT tokyocabinet.TDBQRY/QCNUMLT)

(def STR-EQ tokyocabinet.TDBQRY/QCSTREQ)
(def STR-BEGINSWITH tokyocabinet.TDBQRY/QCSTRBW)
(def STR-ENDSWITH   tokyocabinet.TDBQRY/QCSTREW)
(def STR-INCLUDES   tokyocabinet.TDBQRY/QCSTRINC)

(def STR-REGEX tokyocabinet.TDBQRY/QCSTRRX)
(def STR-OREQ  tokyocabinet.TDBQRY/QCSTROREQ)
(def STR-OR    tokyocabinet.TDBQRY/QCSTROR)
(def STR-AND   tokyocabinet.TDBQRY/QCSTRAND)

(def STR-FULL-AND      tokyocabinet.TDBQRY/QCFTSAND)
(def STR-FULL-OR       tokyocabinet.TDBQRY/QCFTSOR)
(def STR-FULL-PHRASE   tokyocabinet.TDBQRY/QCFTSPH)
(def STR-FULL-COMPOUND tokyocabinet.TDBQRY/QCFTSEX)
 
(def SORT-NUM-ASC tokyocabinet.TDBQRY/QONUMASC)
(def SORT-NUM-DESC tokyocabinet.TDBQRY/QONUMDESC)
(def SORT-TEXT-ASC tokyocabinet.TDBQRY/QOSTRASC)
(def SORT-TEXT-DESC tokyocabinet.TDBQRY/QOSTRDESC)

;; map the operators to the constants
(def numeric-map { "<" NUM-LT ">" NUM-GT "<=" NUM-LE ">=" NUM-GE "=" NUM-EQ })

(def lexical-map 
     { "=" STR-EQ "beginswith" STR-BEGINSWITH "endswith" STR-ENDSWITH "includes" STR-INCLUDES 
       "regex" STR-REGEX "is-token" STR-OREQ "any-token" STR-OR "all-tokens" STR-AND 
       "all-tokens-full" STR-FULL-AND "any-token-full" STR-FULL-OR "phrase-full" STR-FULL-PHRASE 
       "all-compond" STR-FULL-COMPOUND })

;; query -- creates a tokyo cabinet query object
(defn- query-table 
  "Internal.  Construct a Toyko Cabinet query object."
  [query]
  (check-type :table)
  (let [q (tokyocabinet.TDBQRY. (*tokyo* :connection))
        numeric? (fn [x] (or (integer? x) (float? x)))
        kw (into {} (filter #(= (count %) 2) query))
        query (filter #(= (count %) 3) query)]
    (if (:limit kw) (.setlimit q (:limit kw) (:skip kw 0)))
    (if (:sort  kw) (.setorder q (str (:sort kw)) (:order kw SORT-TEXT-ASC)))
    (doseq [c query]
      (let [[field op val] c
            m (if (numeric? val) numeric-map lexical-map)]
          (.addcond q (str field) (m op) (str val))))
    q))


(defn- convert-hash-map 
  "Internal.  Convert a hash map, transforming strings that start with ':' to keywords."
  [hm]
  (let [maybe-keyword (fn [[a b]] (if (= (first a) \:) [(keyword (.substring a 1)) b] [a b]))]
    (into {} (map maybe-keyword hm))))

(defn with-query-results* [query fn]
  (let [q (query-table query) 
        rows (.search q) 
        ;; little bit of magic here -- we automatically transform keys
        ;; that start with ":" to keywords.
        transform-fn ({ :table convert-hash-map } (*tokyo* :type) identity)]
    (binding [*query* q]
      (doseq  [row rows]
        (fn (transform-fn (get-value (String. row))))))))

(defmacro with-query-results 
  "Given a free variable and a query in the form of [[fieldname op
  value] ...], construct a result set and call body with the free
  variable bound to every successive row.  You may also have [:limit
  n] to limit the number of rows returned, [:skip n] to skip the first
  n rows, [:sort fieldname] to sort on a field, and [:order v] to set
  the sort order."  
  [var query & body]
   `(with-query-results* ~query (fn [~var] ~@body)))

(defn hint "Get the query hint for the current query." []
  (.hint *query*))

;;
;; kwic handling
;;

(def KWIC-BRACKETS    tokyocabinet.TDBQRY/KWMUBRCT)
(def KWIC-CTRL        tokyocabinet.TDBQRY/KWMUCTRL)
(def KWIC-TABS        tokyocabinet.TDBQRY/KWMUTAB)
(def KWIC-NO-OVERLAP  tokyocabinet.TDBQRY/KWNOOVER)
(def KWIC-PICKUP-LEAD tokyocabinet.TDBQRY/KWPULEAD)

(defn kwic 
  "Given a row and a field name, returns an array of strings marked up
   with the search results.  Optionally, provide :width, which returns
   the context width for the string, or :marker, which defaults to
   using brackets to demarcate hits (KWIC-BRACKETS) but can also be
   set to KWIC-CTRL (control characters) or KWIC-TABS (tab
   characters).  Mode can also specify no overlapping matches
   (KWIC-NO-OVERLAP) and to pick up the lead (KWIC-PICKUP-LEAD)"
  [row field & { :keys [width marker] :or { width 40 marker KWIC-BRACKETS }} ]
  (.kwic *query* row (str field) width marker))

;; 
;; miscellaneous
;;

(defn- primary-keys* []
  (lazy-seq
    (let [nextkey (.iternext2 (*tokyo* :connection))]
      (when nextkey (cons nextkey (primary-keys*))))))

(defn primary-keys 
  "Return the primary keys for the cabinet.  Lazy."
  []
  (let [result (.iterinit (*tokyo* :connection))]
    (if result
      (primary-keys*)
      nil)))

