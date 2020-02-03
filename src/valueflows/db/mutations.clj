;; TODO

;; Copyright (C) 2019- Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

;; This program is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

;; This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.

;; Additional permission under GNU AGPL version 3 section 7.

;; If you modify this Program, or any covered work, by linking or combining it with any library (or a modified version of that library), containing parts covered by the terms of EPL v 1.0, the licensors of this Program grant you additional permission to convey the resulting work. Your modified version must prominently offer all users interacting with it remotely through a computer network (if your version supports such interaction) an opportunity to receive the Corresponding Source of your version by providing access to the Corresponding Source from a network server at no charge, through some standard or customary means of facilitating copying of software. Corresponding Source for a non-source form of such a combination shall include the source code for the parts of the libraries (dependencies) covered by the terms of EPL v 1.0 used as well as that of the covered work.

(ns valueflows.db.mutations
  (:refer-clojure :exclude (format))
  (:require [taoensso.timbre :as log]
            [fxc.core :as fxc]
            [clj-time.core :as t]
            [clj-storage.core :as storage]
            [valueflows.stores :refer [stores]]
            [java-time :refer [format local-date]]))

(def actions #{:offer :commit :transfer :use :consume :produce :work :exchange})

(defn create-economic-event
  [action resource-quantity-has-numerical-value resource-quantity-has-unit params]
  (let [
        resourceConformsTo (or (:resourceConformsTo params) nil)
        resourceDescription (or (:resourceDescription params) nil)
        provider (or (:provider params) nil)
        receiver (or (:receiver params) nil)
        has-point-in-time (if-let [has-point-in-time (:hasPointInTime params)] has-point-in-time (format "MM/dd" (local-date)))
        note (or (:note params) "")
        resource-inventoried-as (or (:resourceInventoriedAs params) nil)
        current-location (or (:currentLocation params) nil)
        resource-classified-as (or (:resourceClassifiedAs params) [])
        resource-inventoried-as (or (:resourceInventoriedAs params) nil)
        to-resource-inventoried-as (or (:toResourceInventoriedAs params) nil)
        economic-event-id (or (:economicEventId params) (fxc.core/generate 32))
        input-of (or (:inputOf params) nil)
        output-of (or (:outputOf params) nil)
        satisfies (or (:satisfies params) nil)
        economic-event {:_id (str has-point-in-time "-"  (fxc.core/generate 32))
                        :economicEventId economic-event-id
                        :note note
                        :resourceDescription resourceDescription
                        :provider provider
                        :receiver receiver
                        :action action
                        :resourceQuantityHasNumericalValue resource-quantity-has-numerical-value
                        :resourceQuantityHasUnit resource-quantity-has-unit
                        :resourceClassifiedAs resource-classified-as
                        :inputOf input-of
                        :outputOf output-of
                        :currentLocation current-location
                        :satisfies satisfies
                        :resourceInventoriedAs resource-inventoried-as
                        :toResourceInventoriedAs to-resource-inventoried-as
                        :resourceConformsTo resourceConformsTo
                        }
        ]
    (storage/store! (:transaction-store stores) :_id economic-event)
    )
  )



(defn create-intent
  [action availableQuantityHasNumericalValue availableQuantityHasUnit description params]
  (let [hasPointInTime (if-let [hasPointInTime (:hasPointInTime params)] hasPointInTime (format "MM/dd" (local-date)))
        provider (or (:provider params) nil)
        receiver (or (:receiver params) nil)
        intentId (or (:intentId params) (fxc.core/generate 32))
        atLocation (or (:atLocation params) nil)
        ;; resourceClassifiedAs (or (:resource-classified-as params) [])
        resourceConformsTo (or (:resourceConformsTo params) nil)

        intent {:_id (str hasPointInTime "-" (fxc.core/generate 32))
                :intentId intentId
                :provider provider
                :receiver receiver
                :hasPointInTime hasPointInTime
                :action action
                :availableQuantityHasNumericalValue availableQuantityHasNumericalValue
                :availableQuantityHasUnit availableQuantityHasUnit
                :description description
                :atLocation atLocation
                ;; :resourceClassified-as resource-classified-as
                :resourceConformsTo resourceConformsTo
                }]
    (storage/store! (:intent-store stores) :_id intent))
    )


(defn create-process
  [name params]
  (let [has-point-in-time (if-let [has-point-in-time (:hasPointInTime params)] has-point-in-time (format "MM/dd" (local-date)))
        note (or (:note params) "")
        before (or (:before params) nil)
        process {:_id (fxc.core/generate 32)
                 :name name
                 :note note
                 :before before
                 :hasPointInTime has-point-in-time
                 :processId (or (:processId params) (fxc.core/generate 32))
                 }
        ]
    (storage/store! (:process-store stores) :_id process)
    )
  )
