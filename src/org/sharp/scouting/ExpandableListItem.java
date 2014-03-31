package org.sharp.scouting;

import java.util.ArrayList;
import java.util.List;

public class ExpandableListItem<T extends Object>
{

    private String name;
    private List<List<T>> contents;

    private List<Boolean> selectable;

    public ExpandableListItem(String name)
    {
        this.name = name;
        contents = new ArrayList<List<T>>(1);
        selectable = new ArrayList<Boolean>(1);
    }

    public ExpandableListItem(String name, List<List<T>> contents,
                              List<Boolean> select)
    {
        this.name = name;
        this.contents = contents;
        selectable = select;
    }

    public List<List<T>> getContents()
    {
        return contents;
    }

    public List<T> getRow(int row)
    {
        return contents.get(row);
    }

    public T get(int row, int col)
    {
        return contents.get(row).get(col);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setContents(List<List<T>> contents)
    {
        this.contents = contents;
    }

    public void setSelectable(int index, boolean select)
    {
        if(selectable.size() > index)
        {
            selectable.set(index, select);
        }
    }

    public boolean isSelectable(int index)
    {
        if(selectable.size() > index)
        {
            return selectable.get(index);
        }
        else
        {
            return false;
        }
    }

}
