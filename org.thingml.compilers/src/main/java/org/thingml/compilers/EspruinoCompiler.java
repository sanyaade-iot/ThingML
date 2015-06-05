/**
 * Copyright (C) 2014 SINTEF <franck.fleurey@sintef.no>
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingml.compilers;

import org.sintef.thingml.*;
import org.sintef.thingml.constraints.ThingMLHelpers;
import org.thingml.compilers.actions.ActionCompiler;
import org.thingml.compilers.actions.EspruinoActionCompiler;
import org.thingml.compilers.actions.JSActionCompiler;
import org.thingml.compilers.api.ApiCompiler;
import org.thingml.compilers.api.JavaScriptApiCompiler;
import org.thingml.compilers.behavior.BehaviorCompiler;
import org.thingml.compilers.behavior.JSBehaviorCompiler;
import org.thingml.compilers.build.BuildCompiler;
import org.thingml.compilers.build.JSBuildCompiler;
import org.thingml.compilers.cep.CepCompiler;
import org.thingml.compilers.cep.JSCepCompiler;
import org.thingml.compilers.connectors.ConnectorCompiler;
import org.thingml.compilers.connectors.JS2Kevoree;
import org.thingml.compilers.connectors.JS2NodeRED;
import org.thingml.compilers.main.JSMainGenerator;
import org.thingml.compilers.main.MainGenerator;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ffl on 25.11.14.
 */
public class EspruinoCompiler extends JavaScriptCompiler {

   public EspruinoCompiler() {
        super(new EspruinoActionCompiler(), new JavaScriptApiCompiler(), new JSMainGenerator(), new JSBuildCompiler(), new JSBehaviorCompiler(),new JSCepCompiler());
    }

    public EspruinoCompiler(ActionCompiler actionCompiler, ApiCompiler apiCompiler, MainGenerator mainCompiler, BuildCompiler buildCompiler, BehaviorCompiler behaviorCompiler, CepCompiler cepCompiler) {
        super(actionCompiler, apiCompiler, mainCompiler, buildCompiler, behaviorCompiler, cepCompiler);
    }

    @Override
    public ThingMLCompiler clone() {
        return new EspruinoCompiler();
    }

    @Override
    public String getPlatform() {
        return "javascript";
    }

    @Override
    public String getName() {
        return "Javascript for Espruino";
    }

    public String getDescription() {
        return "Generates Javascript code for the Espruino platform.";
    }
}
