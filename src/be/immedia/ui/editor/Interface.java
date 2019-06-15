/*
Copyright (C) 2005 defimedia sa

This file is part of AToms.

AToms is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, version 2 of the License.

AToms is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
version 2 along with AToms; see the file LICENSE.  If not, see
<http://www.gnu.org/licenses/> or write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package be.immedia.ui.editor;

import java.awt.Font;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import fr.imag.usybus_widgets_tracker.JFrameConnector;
import be.immedia.editor.AbstractModule;

public final class Interface
{
	public static void main(String[] args)
	{
		try
		{
      UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      JDialog.setDefaultLookAndFeelDecorated(true);
      AbstractModule.init(Font.SANS_SERIF,12);

      Editor edit=new Editor();
      if(args.length>0){
      	try{
      		edit.loadForm(args[0]);
      	}catch(Exception ex){
          ex.printStackTrace();
          JOptionPane.showMessageDialog(edit,ex.toString(),"Erreur",JOptionPane.ERROR_MESSAGE);
      	}
      }
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
