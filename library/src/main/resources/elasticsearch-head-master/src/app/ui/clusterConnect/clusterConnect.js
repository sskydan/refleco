(function( $, app, i18n ) {

	var ui = app.ns("ui");

	ui.ClusterConnect = ui.AbstractWidget.extend({
		init: function() {
			this._super();
			this.cluster = this.config.cluster;
			this.el = $(this._main_template());
			this.nameEl = this.el.find(".uiClusterConnect-name");
			this.statEl = this.el.find(".uiClusterConnect-status");
			this.statEl.text( i18n.text("Header.ClusterNotConnected") ).css("background", "grey");
			this.cluster.get( "", this._node_handler );
			this.cluster.get( "_cluster/health", this._health_handler );
		},
		
		_node_handler: function(data) {
			if(data) {
				this.nameEl.text(data.name);
				localStorage["base_uri"] = this.cluster.base_uri;
			}
		},
		
		_health_handler: function(data) {
			if(data) {
				this.statEl
					.text( i18n.text("Header.ClusterHealth", data.status, data.number_of_nodes, data.active_primary_shards ) )
					.css( "background", data.status );
			}
		},
		
		_reconnect_handler: function() {
			var base_uri = this.el.find(".uiClusterConnect-uri").val();
			$("body").empty().append(new app.App("body", { id: "es", base_uri: base_uri }));
		},
		
		_main_template: function() {
			return { tag: "SPAN", cls: "uiClusterConnect", children: [
				{ tag: "INPUT", type: "text", cls: "uiClusterConnect-uri", onkeyup: function( jEv ) {
					if(jEv.which === 13) {
						jEv.preventDefault();
						this._reconnect_handler();
					}
				}.bind(this), id: this.id("baseUri"), value: this.cluster.base_uri },
				{ tag: "BUTTON", type: "button", text: i18n.text("Header.Connect"), onclick: this._reconnect_handler },
				{ tag: "SPAN", cls: "uiClusterConnect-name" },
				{ tag: "SPAN", cls: "uiClusterConnect-status" }
			]};
		}
	});

})( this.jQuery, this.app, this.i18n );

