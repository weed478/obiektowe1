package agh.ics.oop.gui;

import agh.ics.oop.core.Rect;
import agh.ics.oop.core.Vector2d;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;

import java.util.List;

public class GridBuilder {

    private static final int GRID_SIZE = 10;
    private final IDrawableMap map;

    public GridBuilder(IDrawableMap map) {
        this.map = map;
    }

    public GridPane buildGrid() {
        Rect bounds = map.getDrawingBounds();

        GridPane gridPane = new GridPane();

        for (int i = 0; i < bounds.width(); i++) {
            gridPane.getColumnConstraints().add(new ColumnConstraints(GRID_SIZE));
        }

        for (int i = 0; i < bounds.height(); i++) {
            gridPane.getRowConstraints().add(new RowConstraints(GRID_SIZE));
        }

        for (int x = bounds.getBL().x; x < bounds.getTR().x; x++) {
            for (int y = bounds.getBL().y; y < bounds.getTR().y; y++) {
                Vector2d mapPos = new Vector2d(x, y);
                Vector2d gridPos = mapToGrid(mapPos);

                List<IDrawableElement> drawables = map.getDrawablesAt(mapPos);

                StackPane field = new StackPane();
                for (IDrawableElement drawable : drawables) {
                    Node node = drawable.getDrawableNode(GRID_SIZE);
                    field.getChildren().add(node);
                }

                GridPane.setHalignment(field, HPos.CENTER);
                gridPane.add(field, gridPos.x, gridPos.y);
            }
        }

        return gridPane;
    }

    private Vector2d flipY(Vector2d v) {
        return new Vector2d(v.x, map.getDrawingBounds().height() - 1 - v.y);
    }

    private Vector2d mapToGrid(Vector2d v) {
        v = v.subtract(map.getDrawingBounds().getBL());
        v = flipY(v);
        return v;
    }
}
