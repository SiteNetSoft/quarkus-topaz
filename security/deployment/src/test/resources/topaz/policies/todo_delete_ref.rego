package todoApp.DELETE.todos.ref

import future.keywords.if

default allowed = false

allowed if {
	ds.check({
		"object_type": "todo",
		"object_id": input.resource.todo.id,
		"relation": "owner",
		"subject_type": "user",
		"subject_id": input.user.id,
	})
}
