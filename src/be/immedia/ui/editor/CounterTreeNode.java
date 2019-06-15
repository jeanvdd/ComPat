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

import java.math.BigInteger;
import java.util.*;

import javax.swing.tree.*;

import be.immedia.util.StringHelper;

final class CounterTreeNode
	implements TreeNode
{
  private static final BigInteger DIX=BigInteger.valueOf(10);

  private final CounterTreeNode parent;
	private final BigInteger v;
  private final String string;
  
  CounterTreeNode()
  {
    this(null,BigInteger.ZERO);
  }
  
	private CounterTreeNode(CounterTreeNode parent,BigInteger v)
  { this.parent=parent; this.v=v; string=v.toString()+"= "+StringHelper.toString(v); }
  
	public String toString() { return string; }

  public TreeNode getChildAt(int childIndex)
  {
    return new CounterTreeNode(this,v.multiply(DIX).add(BigInteger.valueOf(1+childIndex)));
  }

  public int getChildCount() { return 10; }

  public TreeNode getParent() { return parent; }

  public int getIndex(TreeNode node)
  {
		return v.subtract(BigInteger.ONE).mod(DIX).intValue(); 
  }

  public boolean getAllowsChildren() { return true; }
  public boolean isLeaf() { return false; }

  public Enumeration children()
  {
    Vector children=new Vector(10);
    BigInteger b=v.multiply(DIX);
    for(int i=1;i<=10;i++)
    {
      b=b.add(BigInteger.ONE);
      children.add(new CounterTreeNode(this,b));
    }
    return children.elements();
  }

}
