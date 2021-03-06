package org.thingml.xtext.validation.checks

import java.util.Set
import org.eclipse.xtext.validation.Check
import org.thingml.xtext.thingML.Configuration
import org.thingml.xtext.thingML.Property
import org.thingml.xtext.thingML.Thing
import org.thingml.xtext.thingML.ThingMLPackage
import org.thingml.xtext.validation.ThingMLValidatorCheck

class PropertyInitialization extends ThingMLValidatorCheck {
	
	def Set<Property> getUninitializedProperties(Thing t) {
		val props = newHashSet()
		// Properties from current thing
		t.properties.forEach[prop|
			if (prop.init === null)
				props.add(prop);
		]
		
		// Properties from included things
		t.includes.forEach[inc | props.addAll(getUninitializedProperties(inc))]
		
		// Remove properties initialised by set statements
		t.assign.forEach[propAssign|
			props.removeIf(prop | prop === propAssign.property)
		]
		
		return props
	}
	
	@Check(NORMAL)
	def checkPropertyInitialization(Configuration cfg) {
		cfg.instances.forEach[inst, i|
			val props = getUninitializedProperties(inst.type)
			
			// Remove properties initialised by set statements
			cfg.propassigns.forEach[propAssign|
				props.removeIf(prop | prop === propAssign.property)
			]
			
			if (!props.empty) {
				val msg = props.join("Properties (",", ",") are not initialized")[it.name]
				warning(msg, cfg, ThingMLPackage.eINSTANCE.configuration_Instances, i, "properties-not-initialized")
			}
		]
	}
}