package org.sitenetsoft.quarkus.topaz.deployment.devui;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import org.sitenetsoft.quarkus.topaz.runtime.devui.TopazJsonRpcService;

import java.util.Optional;

class TopazDevUIProcessor {

    private static final String CONSOLE_URL = "quarkus.topaz.console-url";
    private static final String ICON = "font-awesome-solid:shield-halved";

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    CardPageBuildItem card(Optional<DevServicesLauncherConfigResultBuildItem> devServices) {
        CardPageBuildItem card = new CardPageBuildItem();
        devServices.map(d -> d.getConfig().get(CONSOLE_URL)).ifPresent(url -> card.addPage(Page.externalPageBuilder("Topaz console")
                .icon(ICON)
                .url(url, url)
                .isHtmlContent()));
        card.addPage(Page.webComponentPageBuilder()
                .title("Policies")
                .icon("font-awesome-solid:file-shield")
                .componentLink("qwc-topaz-policies.js"));
        card.addPage(Page.webComponentPageBuilder()
                .title("Manifest")
                .icon("font-awesome-solid:diagram-project")
                .componentLink("qwc-topaz-manifest.js"));
        return card;
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem jsonRpc() {
        return new JsonRPCProvidersBuildItem(TopazJsonRpcService.class);
    }
}
