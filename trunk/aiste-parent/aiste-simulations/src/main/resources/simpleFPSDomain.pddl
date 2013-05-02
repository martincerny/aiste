; SimpleFPS PDDL domain v1
;
; DATE   : 1 May 2013
; FILE   : SimpleFPS_PDDL_Domain.txt
; AUTHOR : Michail Papakonstantinou, Stavros Vassos
; EMAIL  : sdi0600151@di.uoa.gr, stavrosv@di.uoa.gr
; WWW    : stavros.lostre.org/sFPS
;
; Modified (simplified) by Martin Cerny for use with AiSTe TestBed
; Also added typing and action costs

(define (domain SimpleFPS_PDDL)
(:requirements :strips :typing :action-costs)
(:types  area poi item - poi oponent - poi waypoint )

; (:fluents total-cost)  
; (:functions (distance ?area1 - area ?area2 - area ?waypoint - waypoint) - number )

(:predicates 
	(npc-at ?area - area)
	(npc-close-to ?point - poi)
    	(npc-covered)
	(npc-uncovered)
	(npc-injured)
	(npc-full-health)
	(npc-not-close-to-point)

        (oponent-wounded)

	
	(point-of-interest ?point - poi ?area - area)

	(cover-point ?point - poi)

	(medikit ?item - item)

	(knife ?item - item)
	
	(gun ?item - item)
	(ammo ?item - item)

        (has-gun)
        (has-knife)
	(has-ammo)        

	

	(connected ?area1 - area ?area2 - area ?waypoint - waypoint)
        (risky-area ?area - area)
        (risky-poi ?poi - poi)
        (risk-taken)
		
)

(:action moving-to-take-position
:parameters (?fromarea ?toarea ?waypoint)
:precondition (and 
		(connected ?fromarea ?toarea ?waypoint)
               	(npc-at ?fromarea)
		(waypoint ?waypoint)
		(open ?waypoint)
		(npc-uncovered)
		(npc-close-to ?waypoint)
		)
:effect (and 
		(npc-at ?toarea)
		(npc-not-close-to-point)
             	(not (npc-at ?fromarea))
		(not (npc-close-to ?waypoint))
                ;(increase (total-cost) (distance ?fromarea ?toarea ?waypoint))
                (increase (total-cost) 3)
                
                (when (risky-area ?toarea) (risk-taken) )
	)
)

(:action move-away-from-point
:parameters (?area - area ?point - poi)
:precondition (and
		(npc-at ?area)
		(point-of-interest ?point ?area)
		(npc-close-to ?point)
		)
:effect (and
		(not (npc-close-to ?point))
		(npc-not-close-to-point)
                (increase (total-cost) 1)
	)
)

(:action move-to-point
:parameters (?area - area ?point - point)
:precondition (and
		(npc-at ?area)
		(point-of-interest ?point ?area)
		(npc-not-close-to-point)
		)
:effect (and
		(npc-close-to ?point)
		(not (npc-not-close-to-point))
                (increase (total-cost) 1)
                (when (risky-poi ?point) (risk-taken) )
	)
)

(:action move-to-point-from-point
:parameters (?area - area ?point - poi ?previouspoint - poi)
:precondition (and
		(npc-at ?area)
		(point-of-interest ?point ?area)
		(point-of-interest ?previouspoint ?area)
		(npc-close-to ?previouspoint)
		)
:effect (and
		(npc-close-to ?point)
		(not (npc-close-to ?previouspoint))
		(not (npc-not-close-to-point))
                (increase (total-cost) 1)
	)
)


(:action take-cover
:parameters (?area - area ?point - poi)
:precondition (and
		(npc-at ?area)
		(point-of-interest ?point ?area)
		(cover-point ?point)
		(npc-close-to ?point)
		(npc-uncovered)
		)
:effect (and
		(npc-covered)
		(not (npc-uncovered))
                (increase (total-cost) 0) ; This action takes no time, but is needed to ensure correct planning
	)
)

(:action uncover
:parameters (?point - poi)
:precondition (and
		(npc-covered)
		(npc-close-to ?point)
		)
:effect (and
		(not (npc-covered))
		(npc-uncovered)
		(not (npc-close-to ?point))
                (increase (total-cost) 0) ; This action takes no time, but is needed to ensure correct planning
	)
)

(:action take-ammo
:parameters (?area ?item)
:precondition (and 
		(npc-at ?area)
               	(point-of-interest ?item ?area)
		(npc-close-to ?item)
		(ammo ?item)
		)
:effect (and 
		(not (point-of-interest ?item ?area))
             	(has-ammo)
		(not (npc-close-to ?item))
		(npc-not-close-to-point)
                (increase (total-cost) 0) ; This action takes no time, but is needed to ensure correct planning
	)
)

(:action take-medikit
:parameters (?area ?item)
:precondition (and 
		(npc-at ?area)
               	(point-of-interest ?item ?area)
		(npc-close-to ?item)
		(medikit ?item)
		(npc-injured)
		)
:effect (and 
		(not (point-of-interest ?item ?area))
		(npc-full-health)
		(not (npc-close-to ?item))
		(npc-not-close-to-point)
                (increase (total-cost) 0) ; This action takes no time, but is needed to ensure correct planning
	)
)

(:action take-knife
:parameters (?area ?item)
:precondition (and 
		(npc-at ?area)
               	(point-of-interest ?item ?area)
		(npc-close-to ?item)
		(knife ?item)
		)
:effect (and 
		(not (point-of-interest ?item ?area))
             	(has-knife)
		(not (npc-close-to ?item))
		(npc-not-close-to-point)
                (increase (total-cost) 0) ; This action takes no time, but is needed to ensure correct planning
	)
)

(:action take-gun
:parameters (?area ?item)
:precondition (and 
		(npc-at ?area)
               	(point-of-interest ?item ?area)
		(npc-close-to ?item)
		(gun ?item)
		)
:effect (and 
		(not (point-of-interest ?item ?area))
             	(has-gun)
		(not (npc-close-to ?item))
		(npc-not-close-to-point)
                (increase (total-cost) 0) ; This action takes no time, but is needed to ensure correct planning
	)
)



(:action attack-melee
:parameters  (?area ?knife ?oponent)
:precondition (and
		(npc-at ?area)
		(point-of-interest ?oponent ?area)
		(has-knife)
                (knife ?knife)
		(npc-close-to ?oponent)
		(oponent ?oponent)
		(npc-uncovered)
		)
:effect (and 
            (oponent-wounded)
            (increase (total-cost) 1) 
        )
)

(:action attack-ranged
:parameters  (?npcarea ?gun ?oponent)
:precondition (and
		(npc-at ?npcarea)
		(point-of-interest ?oponent ?npcarea)
               	(oponent ?oponent)
		(gun ?gun)
		(has-ammo)
                (npc-holding ?gun)
		)
:effect (and
		(oponent-wounded)
		(not (has-ammo))
                (increase (total-cost) 1) 
	)
		
)


)
