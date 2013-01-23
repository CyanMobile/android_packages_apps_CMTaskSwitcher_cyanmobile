/*
**
** Copyright 2012, Hussein Ala
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**       http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package com.cyanmobile.TaskSwitcher;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public class App {
	public String name;
	public String pkgName;
        public Intent intent;
	public Drawable icon;
	public Bitmap bitmap;
        public int ids;

	public App() {
		
	}
	
	public App(String name, String pkgName, int ids, Intent intent, Drawable icon, Bitmap bitmap) {
		this.name = name;
		this.pkgName = pkgName;
                this.ids = ids;
                this.intent = intent;
		this.icon = icon;
		this.bitmap = bitmap;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
}
