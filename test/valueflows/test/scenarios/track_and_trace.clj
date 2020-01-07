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
                           (fact "An unknown agent transfer some textile material to Waste Management"
                                 (mut/create-economic-event {:id 1
                                                             :provider :not-relevant
                                                             :receiver :waste-management
                                                             :has-point-in-time (time/now)
                                                             :action :transfer
                                                             :resource-quantity-numeric-value 200
                                                             :resource-quantity-unit :kilo
                                                             :resource-inventory-as "Lot 173 textile material"
                                                             :resource-conforms-to [:red :cotton]})
                                 (-> (q/query-economic-event {:receiver "waste-management"})
                                     first
                                     :resource-inventory-as) => "Lot 173 textile material")
                           (fact "WasteManagement broadcasts an offer to transfer Lot173"
                                 (mut/create-intent {:id 2
                                                     :provider :waste-management
                                                     :action :transfer
                                                     :resource-quantity-numeric-value 200
                                                     :resource-quantity-unit :kilo
                                                     :resource-inventory-as "Lot 173 textile material"
                                                     :has-point-in-time (time/now)})
                                 (-> (q/query-intent {:provider "waste-management"})
                                     first
                                     :resource-inventory-as) => "Lot 173 textile material")
                           (fact "TextileLab commits to receive the Lot 173 from Waste Management"
                                 (mut/create-commitment
                                  {:id 3
                                   :provider :waste-management
                                   :action :transfer
                                   :resource-quantity-numeric-value 200
                                   :resource-quantity-unit :kilo
                                   :resource-inventory-as "Lot 173 textile material"
                                   :has-point-in-time (time/now)
                                   :receiver :textile-lab})
                                 (-> (q/query-commitment {:provider "waste-management"})
                                     first
                                     :receiver) => "textile-lab")
                           (fact "WasteManagement transfers Lot 173 to TextileLab"
                                 (mut/create-economic-event {:id 4
                                                             :provider :waste-management
                                                             :receiver :textile-lab
                                                             :has-point-in-time (time/now)
                                                             :action :transfer
                                                             :resource-quantity-numeric-value 200
                                                             :resource-quantity-unit :kilo
                                                             :resource-inventory-as "Lot 173 textile material"
                                                             :resource-conforms-to [:red :cotton]})
                                 (-> (q/query-economic-event {:receiver "textile-lab"})
                                     first
                                     :resource-inventory-as) => "Lot 173 textile material")
                           (fact "TextileLab creates a new process to produce a new pair of jeans"
                                 (mut/create-process {:id 4
                                                      :name "Create a new pair of hyper jeans"
                                                      :note "Hyper jeans will be produced using the hyperballad design"
                                                      :before (time/now)})
                                 (-> (q/query-process {:id 4})
                                     :name) => "Create a new pair of hyper jeans")
                           (fact "Worker uses Hyperballad design for the hyper jeans"
                                 (mut/create-economic-event {:id 5
                                                             :provider :worker
                                                             :receiver :textile-lab
                                                             :has-point-in-time (time/now)
                                                             :action :cite
                                                             :input-of 4
                                                             :resource-quantity-numeric-value 1
                                                             :resource-quantity-unit :each
                                                             :resource-inventory-as "hyperballad design"})
                                 (-> (q/query-economic-event {:action "cite"})
                                     first
                                     :resource-inventory-as) => "hyperballad design")
                           (fact "Worker consumes 10kg from Lot 173 from TextileLab inventory"
                                 (mut/create-economic-event {:id 6
                                                             :provider :worker
                                                             :receiver :textile-lab
                                                             :has-point-in-time (time/now)
                                                             :action :consume
                                                             :input-of 4
                                                             :resource-quantity-numeric-value 10
                                                             :resource-quantity-unit :kilo
                                                             :resource-inventory-as "Lot 173 textile material"})
                                 (-> (q/query-economic-event {:action "consume"})
                                     first
                                     :resource-inventory-as) => "Lot 173 textile material")
                           (fact "TextileLab now owns 190kg of Lot 173 in its own inventory"
                                 (-> (q/query-resource {:resource-inventory-as "Lot 173 textile material"})
                                     :resource-quantity-numeric-value) => 190)

                           (fact "Worker logs 4 hours working on the jeans"
                                 (mut/create-economic-event {:id 7
                                                             :provider :worker
                                                             :receiver :textile-lab
                                                             :has-point-in-time (time/now)
                                                             :action :work
                                                             :input-of 4
                                                             :resource-quantity-numeric-value 4
                                                             :resource-quantity-unit :hour
                                                             :resource-inventory-as "Lot 173 textile material"
                                                             :note "Done all the work!"
                                                             :resource-conforms-to [:red :cotton]})
                                 (-> (q/query-economic-event {:receiver "textile-lab"})
                                     first
                                     :resource-inventory-as) => "Lot 173 textile material")
                           (fact "Worker produces 1 new pair of jeans called Hyper"
                                 (mut/create-economic-event {:id 8
                                                             :provider :worker
                                                             :receiver :textile-lab
                                                             :has-point-in-time (time/now)
                                                             :action :produce
                                                             :output-of 4
                                                             :resource-quantity-numeric-value 1
                                                             :resource-quantity-unit :each
                                                             :resource-inventory-as "hyper jeans"})
                                 (-> (q/query-economic-event {:id 8})
                                     :resource-inventory-as) => "hyper jeans")
                           (fact "TextileLab offers to transfer 1 Hyper jeans"
                                 (mut/create-intent {:id 9
                                                     :provider :textile-lab
                                                     :action :transfer
                                                     :resource-quantity-numeric-value 1
                                                     :resource-quantity-unit :each
                                                     :resource-inventory-as "hyper jeans"
                                                     :has-point-in-time (time/now)})
                                 (-> (q/query-intent {:provider "textile-lab"})
                                     first
                                     :resource-inventory-as) => "hyper jeans")
                           (fact "The RetailShop commits to receive the Hyper jeans"
                                 (mut/create-commitment
                                  {:id 10
                                   :provider :textile-lab
                                   :action :transfer
                                   :resource-quantity-numeric-value 1
                                   :resource-quantity-unit :each
                                   :resource-inventory-as "hyper jeans"
                                   :has-point-in-time (time/now)
                                   :receiver :retail-shop})
                                 (-> (q/query-commitment {:provider "textile-lab"})
                                     first
                                     :receiver) => "retail-shop")
                           (fact "TextileLab transfers the Hyper jeans"
                                 (mut/create-economic-event {:id 11
                                                             :provider :textile-lab
                                                             :receiver :retail-shop
                                                             :has-point-in-time (time/now)
                                                             :action :transfer
                                                             :resource-quantity-numeric-value 1
                                                             :resource-quantity-unit :each
                                                             :resource-inventory-as "hyper jeans"})
                                 (-> (q/query-economic-event {:receiver "retail-shop"})
                                     first
                                     :resource-inventory-as) => "hyper jeans")))
