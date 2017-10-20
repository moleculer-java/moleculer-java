"use strict";

const Moleculer = require("moleculer");

module.exports = {

	namespace: "dev",
	nodeID: null,
	
	transporter: "Redis",
		
	cacher: {
		type: "memory",
		opts: {
			ttl: 60
		}
	},
	
	registry: {
		strategy: Moleculer.Strategies.Random,
		preferLocal: true				
	}
	
};