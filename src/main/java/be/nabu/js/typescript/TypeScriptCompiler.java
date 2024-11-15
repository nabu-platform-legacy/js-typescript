/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.js.typescript;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.coveo.nashorn_modules.FilesystemFolder;
import com.coveo.nashorn_modules.Require;

import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import be.nabu.utils.io.IOUtils;

/**
 * More info:
 * 
 * - https://github.com/Microsoft/TypeScript/wiki/Using-the-Compiler-API-(TypeScript-1.4)
 * - https://github.com/theblacksmith/typescript-compiler/blob/bcf9c2e639fe39187ec66d24be99abc3a05fdc3c/src/index.ts
 * - https://www.npmjs.com/package/typescript-compiler
 * - https://github.com/Microsoft/TypeScript
 * 		- https://github.com/Microsoft/TypeScript/tree/master/lib
 * - https://github.com/martypitt/typescript4j/blob/master/src/main/java/com/mangofactory/typescript/TypescriptCompiler.java
 */
public class TypeScriptCompiler {
	
	public static void main(String...args) throws ScriptException, IOException {
//		InputStream input;
//		ScriptEngine engine = getCompiler();
//		Object eval = engine.eval("ts.transpileModule(\"document.body.innerHTML = 'test'\", {});");
//		Object eval;
//		input = TypeScriptCompiler.class.getClassLoader().getResourceAsStream("example-error.ts");
//		try {
//			byte[] bytes = IOUtils.toBytes(IOUtils.wrap(input));
//			String string = new String(bytes);
//			string = string.replaceAll("(?s)[\r\n]+", "'\n\t+'");
//			String parse = "ts.transpileModule('" + string + "', { reportDiagnostics: true, moduleName: 'test' });";
//			eval = engine.eval(parse);
//		}
//		finally {
//			input.close();
//		}
//		
		Date date = new Date();
		report(compile(readFile("example-import.ts")));
		System.out.println("First compile: " + (new Date().getTime() - date.getTime()) + "ms");
//		date = new Date();
//		report(compile(readFile("example-error.ts")));
//		System.out.println("Second compile: " + (new Date().getTime() - date.getTime()) + "ms");
	}
	
	@SuppressWarnings("unchecked")
	public static void report(JSObject object) {
		Map<String, Object> map = (Map<String, Object>) object;
		for (String key : map.keySet()) {
			if (key.equals("diagnostics")) {
				System.out.println("dia: " + ((JSObject) map.get(key)).getSlot(0));
				Map<String, Object> diagnostics = (Map<String, Object>) map.get(key);
				for (String diagnostic : diagnostics.keySet()) {
					Map<String, Object> single = (Map<String, Object>) diagnostics.get(diagnostic);
					for (String wtf : single.keySet()) {
						System.out.println("Diagnostic: " + diagnostic + ": " + wtf + " = " + single.get(wtf));
					}
				}
			}
			else {
				System.out.println(key + " = " + map.get(key));
			}
		}
	}
	
	public static JSObject compile(String content) throws ScriptException, IOException {
		ScriptEngine compiler = getCompiler();
		Map<String, Object> addSourceFiles = addSourceFiles(compiler, Target.ES5);
		Map<String, Object> moduleFiles = new HashMap<String, Object>();
		addSourceFile(compiler, Target.ES5, moduleFiles, "@angular/core/index.d.ts");
		JSObject transpiler = (JSObject) compiler.eval("ts.transpileModule");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("reportDiagnostics", true);
		params.put("moduleName", "test");
		params.put("fileName", "myFile.ts");
		params.put("sourceFiles", addSourceFiles);
		params.put("modules", moduleFiles);
		params.put("defaultLibFileName", Target.ES5.getFiles()[0]);
		return (JSObject) transpiler.call(transpiler, content, params);
	}

	public static interface CompilationResult {
		public String getCompiled();
		public Map<String, Object> getDiagnostics();
	}
	
	public static interface Compiler {
		public CompilationResult compile(String content);
	}
	
	private static ScriptEngine getCompiler() throws ScriptException, IOException {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		Require.enable((NashornScriptEngine) engine, FilesystemFolder.create(new File("/home/alex/tmp2/angular-core"), "UTF-8"));
		InputStream input = TypeScriptCompiler.class.getClassLoader().getResourceAsStream("typescript.js");
		try {
			engine.eval(new InputStreamReader(input, "UTF-8"));
		}
		finally {
			input.close();
		}
		return engine;
	}
	
	private static Map<String, Object> addSourceFiles(ScriptEngine engine, Target target) throws ScriptException {
		Map<String, Object> files = new HashMap<String, Object>();
		for (String file : target.getFiles()) {
			addSourceFile(engine, target, files, file);
		}
		return files;
	}

	private static void addSourceFile(ScriptEngine engine, Target target, Map<String, Object> files, String file) throws ScriptException {
		JSObject eval = (JSObject) engine.eval("ts.createSourceFile");
		// first param is the "this" context
		files.put(file, eval.call(eval, file, readFile(file), target));
	}
	
	public static String readFile(String name) {
		try {
			InputStream input = TypeScriptCompiler.class.getClassLoader().getResourceAsStream(name);
			try {
				byte[] bytes = IOUtils.toBytes(IOUtils.wrap(input));
				return new String(bytes);
			}
			finally {
				input.close();
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public enum Target {
		ES5(1, "lib.d.ts"),
		ES6(2, "lib.es6.d.ts")
		;
		private int value;
		private String[] files;

		private Target(int value, String...files) {
			this.value = value;
			this.files = files;
		}

		public int getValue() {
			return value;
		}

		public String[] getFiles() {
			return files;
		}
	}
}
