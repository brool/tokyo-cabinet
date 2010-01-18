Tokyo Cabinet for Clojure
=========================

Introduction
------------

This is a simple interface to the Tokyo Cabinet libraries.  Tokyo
Cabinet is a very cool, very high performing key-value store.  This
library supports table mode, which essentially means that arbitrary
hashmaps can be stored in the cabinet.

Note that this is appropriate for local storage only -- if you're
looking to share a Tokyo Cabinet to multiple computers, you actually
want Tokyo Tyrant.

Basic Usage
-----------

The with-cabinet call creates/opens a cabinet and allows the use of
the various access routines within the scope of the call.  For
example, here's how to create a cabinet with three entries.

::

    (ns user (:use tokyo-cabinet))  ;; bring into our namespace
    
    (with-cabinet { :filename "test.tokyo" :mode (+ OWRITER OCREAT) } 
        (doseq [[name val] [["1" "one"] ["2" "two"] ["3" "three"]]]
            (put-value name val)))

This creates a Tokyo Cabinet *hash table*, which allows one value per
key.  Now query an entry::

    (with-cabinet { :filename "test.tokyo" :mode OREADER } 
        (get-value "1"))
    "one"

B+ Tree
-------
*B+ tree* allows duplicate values for a key. Each value is made part of
an array which hashes to the same key.

::
    
    (with-cabinet { :filename "test.tokyo" :mode (+ OWRITER OCREAT) :type :bplus } 
        (doseq [[name val] [["language" "clojure"] ["language" "common lisp"] ["OS" "Linux"] ["OS" "Mac"]]]
            (putdup-value name val)))

Query::

    (with-cabinet { :filename "test.tokyo" :mode OREADER } 
        (getdup-value "language"))
    => #<ArrayList [clojure, common lisp]>

    
Tables
------

A *table* in Tokyo Cabinet can be used to store arbitrary hash maps.  For example::

    (def params { :filename "test-table.tokyo" :mode (+ OWRITER OCREAT) :type :table } )
    (with-cabinet params
        (put-value nil { :name "John Doe" :hobbies "rowing fishing skiing" :age 28 :gender "M" })
        (put-value nil { :name "Melissa Swift" :hobbies "soccer tennis books" :age 33 :gender "F"})
        (put-value nil { :name "Tom Swift" :hobbies "inventing exploring" :gender "M" })
        (put-value nil { :name "Harry Potter" :hobbies "magic quidditch flying" :gender "M" :age 9 }))

Since the cabinet was created in :table mode, put-value will take a map instead of a single value.  A nil value for the key automatically generates a unique primary key.

Queries
-------

Queries can be run, and you can use (hint) to take a look at how the query is being performed::

    ; show a hint and all rows matching
    (defn showrows [query]
        (let [showhint (atom false)] 
            (with-query-results row query
                (when (compare-and-set! showhint false true)
                      (println "Query: " query)
                      (println "Hint: " (hint))
                      (println "Results:"))
                (println row)))
            (println))

    (with-cabinet params
        (showrows [[:age ">=" 30]])
        (showrows [[:hobbies "any-token" "soccer"]]))

Leads to the following output::

    Query:  [[:age >= 30]]
    Hint:  scanning the whole table
    result set size: 1
    leaving the natural order

    Results:
    {:gender F, :hobbies soccer tennis books, :name Melissa Swift, :age 33}

    Query:  [[:hobbies any-token soccer]]
    Hint:  scanning the whole table
    result set size: 1
    leaving the natural order

    Results:
    {:gender F, :hobbies soccer tennis books, :name Melissa Swift, :age 33}

Indexes
-------

Indexes can be added with create-index (and removed with delete-index), which help optimize particular queries.

The different index types:

* INDEX-DECIMAL
* INDEX-LEXICAL
* INDEX-QGRAM

With some optional specifiers that can be added / ored in:

* INDEX-KEEP -- keep the index if it already exists
* INDEX-OPTIMIZE

Running the queries again, with indexes:

::

    ; indexes are persistent
    (with-cabinet params
        (create-index :hobbies INDEX-TOKEN)
        (create-index :age INDEX-DECIMAL))

    ; try the queries again with the indexes in place
    (with-cabinet params
        (showrows [[:age ">=" 30]])
        (showrows [[:hobbies "any-token" "soccer"]]))

Gets the following hint::

    Query:  [[:age >= 30]]
    Hint:  using an index: ":age" asc (NUMGT/NUMGE)
    result set size: 1
    leaving the natural order

    Results:
    {:gender F, :hobbies soccer tennis books, :name Melissa Swift, :age 33}

    Query:  [[:hobbies any-token soccer]]
    Hint:  using an index: ":hobbies" inverted (STROR)
    token occurrence: "soccer" 1
    result set size: 1
    leaving the natural order

    Results:
    {:gender F, :hobbies soccer tennis books, :name Melissa Swift, :age 33}

Optional Search Parameters
--------------------------

You can further control what's fetched by using a number of optional
specifiers in the query:

* :limit nnn -- limits the number of rows returned
* :skip  nnn -- skips the first nnn rows
* :sort  fieldname -- sorts by the given field
* :order val -- the specific ordering, one of SORT-NUM-ASC, SORT-NUM-DESC, SORT-TEXT-ASC, or SORT-TEXT-DESC

For example::

    (with-cabinet params (with-query-results row [] (println (:name row))))    
    John Doe
    Melissa Swift
    Tom Swift
    Harry Potter

    (with-cabinet params (with-query-results row [[:sort :name]] (println (:name row))))
    Harry Potter
    John Doe
    Melissa Swift
    Tom Swift

    (with-cabinet params (with-query-results row [[:sort :name] [:order SORT-TEXT-DESC]] (println (:name row))))
    Tom Swift
    Melissa Swift
    John Doe
    Harry Potter

    (with-cabinet params (with-query-results row [[:sort :name] [:order SORT-TEXT-DESC] [:limit 1]] (println (:name row))))
    Tom Swift

Lower Level
-----------

Depending on your application, it might not be convenient to have to
bracket everything with with-cabinet, since that means an open and
close.  You can also use the lower level open-cabinet and
close-cabinet calls, along with the "with" statement.  This is also an
easier way to use it at the command line.  For example::

       (def test-database (open-cabinet { :filename "test-open.tokyo" :mode (+ OWRITER OCREAT) }))
       (with test-database (put-value "1" "one"))
       (with test-database (get-value "1"))
       (with test-database (print (primary-keys)))
       (close-cabinet test-database)

Miscellaneous
-------------

Use (primary-keys) to return a lazy list of primary keys.

::

    (with-cabinet { :filename "test.tokyo" :mode (+ OWRITER OCREATE) :type :table }
        (print (primary-keys)))



Links
-----

* Tokyo Cabinet -- http://tokyocabinet.sourceforge.net/
* Tokyo Cabinet / Java API -- http://tokyocabinet.sourceforge.net/javadoc/
