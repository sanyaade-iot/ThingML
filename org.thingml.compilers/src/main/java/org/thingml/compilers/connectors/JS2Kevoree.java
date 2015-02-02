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
package org.thingml.compilers.connectors;

import com.eclipsesource.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.sintef.thingml.*;
import org.thingml.compilers.Context;
import org.thingml.compilers.build.BuildCompiler;
import org.thingml.compilers.main.JSMainGenerator;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Created by bmori on 27.01.2015.
 */
public class JS2Kevoree extends ConnectorCompiler {

    @Override
    public void generateLib(Context ctx, Configuration cfg) {
        //copy Gruntfile.js
        try {
            final InputStream input = this.getClass().getClassLoader().getResourceAsStream("javascript/lib/Gruntfile.js");
            final List<String> pomLines = IOUtils.readLines(input);
            String pom = "";
            for(String line : pomLines) {
                pom += line + "\n";
            }
            input.close();
            final PrintWriter w = new PrintWriter(new FileWriter(new File(ctx.getOutputDir() + "/" + cfg.getName() +  "/Gruntfile.js")));
            w.println(pom);
            w.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Update package.json
        try {
            final InputStream input = new FileInputStream(ctx.getOutputDir() + "/" + cfg.getName() + "/package.json");
            final List<String> packLines = IOUtils.readLines(input);
            String pack = "";
            for (String line : packLines) {
                pack += line + "\n";
            }
            input.close();

            final JsonObject json = JsonObject.readFrom(pack);
            final JsonObject deps = json.get("dependencies").asObject();
            deps.add("kevoree-entities", "^7.0.0");
            final JsonObject devDeps = json.get("devDependencies").asObject();
            devDeps.add("grunt", "^0.4.1");
            devDeps.add("grunt-kevoree", "^5.0.0");
            devDeps.add("grunt-kevoree-genmodel", "^2.0.0");
            devDeps.add("grunt-kevoree-registry", "^2.0.0");
            final JsonObject scripts = json.get("scripts").asObject();
            scripts.add("prepublish", "grunt");
            scripts.add("postpublish", "grunt publish");

            final JsonObject kevProp = JsonObject.readFrom("{\"package\":\"my.package\"}");
            json.add("kevoree", kevProp);

            //FIXME: I can create a package_kev.json which contains all I need, but I cannot update the existing package.json. No exception is thrown, but the original file is not updated...
            final File f = new File(ctx.getOutputDir() + "/" + cfg.getName() + "/package.json");
            System.out.println("Can write? " + f.canWrite());
            final OutputStream output = new FileOutputStream(f);
            System.out.println("JSON: " + json.toString());
            System.out.println("BSON: " + json.toString().getBytes().length);
            IOUtils.write(json.toString(), output);
            IOUtils.closeQuietly(output);
            //IOUtils.copy(new FileInputStream(ctx.getOutputDir() + "/" + cfg.getName() + "/package_kev.json"), new FileOutputStream(ctx.getOutputDir() + "/" + cfg.getName() + "/package.json"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Generate wrapper
        StringBuilder builder = ctx.getBuilder(cfg.getName() + "/" + cfg.getName() + ".js" );
        builder.append("var Connector = require('./Connector');\n");
        builder.append("var AbstractComponent = require('kevoree-entities').AbstractComponent;\n");

        for(Thing t : cfg.allThings()) {
            builder.append("var " + t.getName() + " = require('./" + t.getName() + "');\n");
        }

        builder.append("/**\n* Kevoree component\n* @type {" + cfg.getName() +  "}\n*/\n");
        builder.append("var " + cfg.getName() + " = AbstractComponent.extend({\n");
        builder.append("toString: 'TestTimerJS',\n");

        //TODO: generate dictionnay for attributes

        builder.append("construct: function() {\n");
        for(Instance i : cfg.danglingPorts().keySet()) {
            builder.append("this." + i.getName() + " = null;\n");
        }
        builder.append("},\n\n");


        builder.append("start: function (done) {\n");
        builder.append("this._super(function () {\n");
        JSMainGenerator.generateInstances(cfg, builder, ctx, true);

        for(Map.Entry e : cfg.danglingPorts().entrySet()) {
            final Instance i = (Instance) e.getKey();
            for(Port p : (List<Port>)e.getValue()) {
                 if (p.getSends().size() > 0) {
                     builder.append("this." + i.getName() + ".get" + ctx.firstToUpper(p.getName()) + "Listeners().push(this.out_" + i.getName() + "_" + p.getName() + "_out.bind(this));\n");
                 }
            }
        }

        for(Instance i : cfg.danglingPorts().keySet()) {
            builder.append("this." + i.getName() + "._init();\n");
        }

        builder.append("done();\n");
        builder.append("}.bind(this));\n");
        builder.append("},\n\n");


        builder.append("stop: function (done) {\n");
        builder.append("this._super(function () {\n");
        for(Instance i : cfg.allInstances()) {
            builder.append(i.getName() + "._stop();\n");
        }
        builder.append("done();\n");
        builder.append("}.bind(this));\n");
        builder.append("}");

        //int id = 0;

        for(Map.Entry e : cfg.danglingPorts().entrySet()) {
            final Instance i = (Instance) e.getKey();
            for(Port p : (List<Port>)e.getValue()) {
                if (p.getReceives().size() > 0) {
                    builder.append(",\nin_" + i.getName() + "_" + p.getName() + "_in: function (msg) {\n");
                    builder.append("var json = JSON.parse(msg);\n");
                    int id = 0;
                    for(Message m : p.getReceives()) {
                        if (id > 0) {
                            builder.append("else ");
                        }
                        builder.append("if (json.message === '" + m.getName() + "') {\n");
                        builder.append("this." + i.getName() + ".receive" + m.getName() + "On" + p.getName() + "(");
                        int j = 0;
                        for(Parameter param : m.getParameters()) {
                            if (j > 0) {
                                builder.append(", ");
                            }
                            builder.append("json." + param.getName());
                            j++;
                        }
                        builder.append(");\n");
                        builder.append("}\n");
                        id++;
                    }
                    builder.append("}");
                }
            }
        }

        for(Map.Entry e : cfg.danglingPorts().entrySet()) {
            final Instance i = (Instance) e.getKey();
            for(Port p : (List<Port>)e.getValue()) {
                if (p.getSends().size() > 0) {
                    builder.append(",\nout_" + i.getName() + "_" + p.getName() + "_out: function(msg) {/* This will be overwritten @runtime by Kevoree JS */}");
                }
            }
        }
        builder.append("});\n\n");
        builder.append("module.exports = " + cfg.getName() + ";\n");
    }
}
