package todoApp.DELETE.secured.todos.__id

import future.keywords.if

default allowed = false

allowed if {
	ds.check({
		"object_type": "todo",
		"object_id": input.resource.id,
		"relation": "owner",
		"subject_type": "user",
		"subject_id": input.user.id,
	})
}
