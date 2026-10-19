package org.sitenetsoft.quarkus.topaz.security.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.sitenetsoft.quarkus.topaz.security.TopazAuthorize;
import org.sitenetsoft.quarkus.topaz.security.TopazCheck;
import org.sitenetsoft.quarkus.topaz.security.TopazObjectId;
import org.sitenetsoft.quarkus.topaz.security.TopazResource;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class SecuredService {

    public record TodoRef(String id, String note) {
    }

    public record OwnedTodo(@TopazObjectId String id, String note) {
    }

    @TopazAuthorize(path = "todoApp.DELETE.todos.__id")
    public Uni<String> deleteReactive(@TopazResource("id") String id) {
        return Uni.createFrom().item("deleted " + id);
    }

    @TopazAuthorize(path = "todoApp.DELETE.todos.__id")
    public String deleteBlocking(@TopazResource("id") String id) {
        return "deleted " + id;
    }

    @TopazAuthorize(path = "todoApp.DELETE.todos.ref")
    public String deleteRef(@TopazResource("todo") TodoRef ref) {
        return "deleted " + ref.id();
    }

    @TopazAuthorize(path = "todoApp.GET.todos", decision = "allowed")
    public String list() {
        return "ok";
    }

    @TopazCheck(objectType = "todo", relation = "owner")
    public Uni<String> deleteOwned(@TopazObjectId String id) {
        return Uni.createFrom().item("deleted " + id);
    }

    @TopazCheck(objectType = "todo", relation = "owner")
    public String deleteOwnedDto(OwnedTodo todo) {
        return "deleted " + todo.id();
    }

    @TopazCheck(objectType = "todo", relation = "owner", objectId = "todo-1")
    public String deleteFixed() {
        return "deleted todo-1";
    }
}
