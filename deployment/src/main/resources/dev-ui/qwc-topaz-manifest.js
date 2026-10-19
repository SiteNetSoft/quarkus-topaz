import { html, css } from 'lit';
import { QwcHotReloadElement } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';

export class QwcTopazManifest extends QwcHotReloadElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        pre { margin: 0; padding: 12px; overflow: auto; font-family: monospace; }
        .info { padding: 8px 12px; color: var(--lumo-secondary-text-color); }
        .error { padding: 8px 12px; color: var(--lumo-error-text-color); }
    `;

    static properties = {
        _manifest: { state: true },
        _error: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload() {
        this._error = null;
        this.jsonRpc.getManifest()
            .then(r => this._manifest = r.result)
            .catch(e => this._error = (e && e.message) || 'Failed to load the manifest');
    }

    render() {
        if (this._error) {
            return html`<div class="error">${this._error}</div>`;
        }
        if (this._manifest === undefined) {
            return html`<div class="info">Loading manifest...</div>`;
        }
        return html`<pre>${this._manifest}</pre>`;
    }
}
customElements.define('qwc-topaz-manifest', QwcTopazManifest);
