import { html, css } from 'lit';
import { QwcHotReloadElement } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';

export class QwcTopazPolicies extends QwcHotReloadElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        table { width: 100%; border-collapse: collapse; }
        th, td { text-align: left; padding: 8px 12px; border-bottom: 1px solid var(--lumo-contrast-10pct); }
        th { font-weight: 600; color: var(--lumo-secondary-text-color); }
        .info { padding: 8px 12px; color: var(--lumo-secondary-text-color); }
        .error { padding: 8px 12px; color: var(--lumo-error-text-color); }
    `;

    static properties = {
        _connection: { state: true },
        _policies: { state: true },
        _error: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload() {
        this._error = null;
        this.jsonRpc.connection().then(r => this._connection = r.result);
        this.jsonRpc.listPolicies()
            .then(r => this._policies = r.result)
            .catch(e => this._error = (e && e.message) || 'Failed to list policies');
    }

    render() {
        if (this._error) {
            return html`<div class="error">${this._error}</div>`;
        }
        if (!this._policies) {
            return html`<div class="info">Loading policies...</div>`;
        }
        return html`
            ${this._connection ? html`<div class="info">Authorizer ${this._connection.authorizer}, directory ${this._connection.directory}</div>` : ''}
            <table>
                <thead><tr><th>Id</th><th>Package path</th></tr></thead>
                <tbody>
                    ${this._policies.map(p => html`<tr><td>${p.id}</td><td>${p.packagePath}</td></tr>`)}
                </tbody>
            </table>`;
    }
}
customElements.define('qwc-topaz-policies', QwcTopazPolicies);
