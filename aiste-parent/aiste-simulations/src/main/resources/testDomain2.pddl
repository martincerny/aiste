(define (domain TestDomain2)
	(:requirements :strips)
	(:predicates 
		(achieved ?loc)
	)
        (:action achieve 
            :parameters (?loc)
            :precondition (not (achieved ?loc))
            :effect (achieved ?loc)
        )
)