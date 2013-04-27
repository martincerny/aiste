(define (domain TestDomain2)
	(:requirements :strips)
	(:predicates 
                (bagr)
		(achieved ?loc)
	)
        (:action achieve 
            :parameters (?loc)
            :effect (achieved ?loc)
        )
)