;; TODO

;; Copyright (C) 2019- Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

;; This program is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

;; This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.

;; Additional permission under GNU AGPL version 3 section 7.

;; If you modify this Program, or any covered work, by linking or combining it with any library (or a modified version of that library), containing parts covered by the terms of EPL v 1.0, the licensors of this Program grant you additional permission to convey the resulting work. Your modified version must prominently offer all users interacting with it remotely through a computer network (if your version supports such interaction) an opportunity to receive the Corresponding Source of your version by providing access to the Corresponding Source from a network server at no charge, through some standard or customary means of facilitating copying of software. Corresponding Source for a non-source form of such a combination shall include the source code for the parts of the libraries (dependencies) covered by the terms of EPL v 1.0 used as well as that of the covered work.

(ns valueflows.test.scenarios.track-and-trace
  (:require [midje.sweet :refer [against-background before after facts fact =>]]

            [valueflows
             [stores :refer [stores]]]
            [valueflows.db
             [mutations :as mut]
             [queries :as q]]

            [clj-storage.core :as storage :refer [Store]]
            [clj-time.core :as time]
            monger.joda-time
            
            [mount.core :as mount]
            [taoensso.timbre :as log]
            ))

(def actors #{:waste-management :retail-shop :textile-lab :fashion-store})
(def units #{:kilo :each :hour})

(against-background [(before :contents (mount/start-with-args {:port 8888}))
                     (after :contents (do
                                        (storage/empty-db-stores! stores)
                                        (mount/stop)))]

                    (facts "Textile lab scenario"
                           (fact "Several actors produce textile output"
                                 (mut/create-economic-event
                                  {:id 1
                                   :provider :not-relevant 
                                   :receiver :waste-management
                                   :has-point-in-time (time/now)
                                   :action :produce
                                   :resource-quantity-numeric-value 200
                                   :resource-quantity-unit :kilo
                                   :resource-inventory-as "Lot 173 textile material"
                                   :resource-conforms-to [:red :cotton]})
                                 (-> (q/query-economic-event
                                      {:receiver "waste-management"})
                                     first
                                     :resource-inventory-as) => "Lot 173 textile material")))
