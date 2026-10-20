package todoApp.POST.body

import future.keywords.if

default allowed = false

allowed if input.resource.body == "yes"
