// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.canvas.core;

import org.eclipse.core.resources.IFile;
import org.json.JSONObject;

/**
 * @author arian
 *
 */
public class CanvasModel {
	private CanvasMainSettings _settings;
	private WorldModel _world;
	private IFile _file;

	public CanvasModel(IFile file) {
		_file = file;
		_settings = new CanvasMainSettings();
		_world = new WorldModel(file);
	}

	public String getClassName() {
		if (_file == null) {
			return "Canvas";
		}

		String name = _file.getName();
		String ext = _file.getFileExtension();
		int end = name.length() - ext.length() - 1;
		return name.substring(0, end);
	}

	public void read(JSONObject data) {
		_settings.read(data.getJSONObject("settings"));
		_world.getAssetTable().read(data.optJSONObject("asset-table"));
		_world.read(data.getJSONObject("world"));
	}

	public void write(JSONObject data) {
		{
			JSONObject data2 = new JSONObject();
			data.put("settings", data2);
			_settings.write(data2);
		}

		{
			JSONObject data2 = new JSONObject();
			data.put("world", data2);
			_world.write(data2, true);
		}

		{
			data.put("asset-table", _world.getAssetTable().toJSON());
		}
	}

	public CanvasMainSettings getSettings() {
		return _settings;
	}

	public WorldModel getWorld() {
		return _world;
	}
}