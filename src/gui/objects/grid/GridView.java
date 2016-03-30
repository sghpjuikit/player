/**
 * Copyright (c) 2013, 2015, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gui.objects.grid;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.css.*;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.input.ScrollEvent;
import javafx.util.Callback;

import util.access.V;
import util.functional.Functors.Ƒ1;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toCollection;
import static javafx.collections.FXCollections.observableArrayList;
import static util.async.Async.runLater;
import static util.functional.Util.stream;

/**
 * A GridView is a virtualised control for displaying items in a
 * visual, scrollable, grid-like fashion. In other words, whereas a ListView
 * shows one {@link ListCell} per row, in a GridView there will be zero or more
 * {@link GridCell} instances on a single row.
 * <p/>
 * This approach means that the number of GridCell instances
 * instantiated will be a significantly smaller number than the number of
 * items in the GridView items list, as only enough GridCells are created for
 * the visible area of the GridView. This helps to improve performance and
 * reduce memory consumption.
 * <p/>
 * Because each {@link GridCell} extends from {@link Cell}, the same approach
 * of cell factories that is taken in other UI controls is also taken in GridView.
 * This has two main benefits:
 *
 * <ol>
 *   <li>GridCells are created on demand and without user involvement,
 *   <li>GridCells can be arbitrarily complex. A simple GridCell may just have
 *   its {@link GridCell#textProperty() text property} set, whereas a more complex
 *   GridCell can have an arbitrarily complex scenegraph set inside its
 *   {@link GridCell#graphicProperty() graphic property} (as it accepts any Node).
 * </ol>
 *
 *
 *
 * @see GridCell
 */
public class GridView<T,F> extends Control {

    final Class<F> type;
    final ObservableList<T> itemsAll;
    final FilteredList<T> itemsFiltered;
    final SortedList<T> itemsSorted;
    final Ƒ1<T,F> filterByMapper;


    /*
     * Predicate that filters the table list. Null predicate will match all
     * items (same as always true predicate). The value reflects the filter
     * generated by the user through the {@link #searchBox}. Changing the
     * predicate programmatically is possible, however the searchBox will not
     * react on the change, its effect will merely be overridden and when
     * search box predicate changes, it will in turn override effect of a
     * custom predicate.
     */
    public final ObjectProperty<Predicate<? super T>> itemsPredicate;
    public final ObjectProperty<Comparator<? super T>> itemsComparator;

    public final V<T> selectedItem = new V<>(null);
    public final V<T> selectedRow = new V<>(null);

    private boolean scrollFlag = true;

    /** Creates a default, empty GridView control. */
    public GridView(Class<F> type) {
        this(type, null, null);
    }

    /** Creates a default, empty GridView control. */
    public GridView(Class<F> type, Ƒ1<T,F> filterByMapper) {
        this(type, filterByMapper, null);
    }

    /** Convenience consturctor. Creates an empty GridView with specified sizes. */
    public GridView(Class<F> type, Ƒ1<T,F> filterByMapper, double cellWidth, double cellHeight, double vgap, double hgap) {
        this(type, filterByMapper, null);
        setCellWidth(cellWidth);
        setCellHeight(cellHeight);
        setHorizontalCellSpacing(hgap);
        setVerticalCellSpacing(vgap);
    }

    /** Convenience constructor. Creates a default GridView with the provided items. */
    public GridView(Class<F> type, Ƒ1<T,F> filterByMapper, ObservableList<T> backingList) {
        this.type = type;
        this.filterByMapper = filterByMapper==null ? x -> (F)x : filterByMapper;

        itemsAll = backingList==null ? observableArrayList() : backingList;
        itemsFiltered = new FilteredList<>(itemsAll);
        itemsSorted = new SortedList<>(itemsFiltered);

        itemsPredicate = itemsFiltered.predicateProperty();
        itemsComparator = itemsSorted.comparatorProperty();

        getStyleClass().add(DEFAULT_STYLE_CLASS);

        // Decrease scrolling speed
        // The default scrolling speed is simply too much. On my system its more than a full
        // vertical 'view' which is very confusing as user loses any indication of scrolling amount.
        // impl: consume scroll events and refire with smaller vertical values
        double factor = 1/3d;
        addEventFilter(ScrollEvent.ANY, e -> {
            if(scrollFlag) {
                Event ne = new ScrollEvent(
                    e.getEventType(),e.getX(),e.getY(),e.getScreenX(),e.getScreenY(),
                    e.isShiftDown(),e.isControlDown(),e.isAltDown(),e.isMetaDown(),e.isDirect(),
                    e.isInertia(),e.getDeltaX(),e.getDeltaY()*factor,e.getTextDeltaX(),e.getTextDeltaY()*factor,
                    e.getTextDeltaXUnits(),e.getTextDeltaX(),e.getTextDeltaYUnits(),e.getTextDeltaY()*factor,
                    e.getTouchCount(),e.getPickResult()
                );
                e.consume();
                scrollFlag = false;
                runLater(() -> {
                    if (e.getTarget() instanceof Node) {
                        ((Node) e.getTarget()).fireEvent(ne);
                    }
                    scrollFlag = true;
                });
            }
        });
    }

    @Override
    protected GridViewSkin<T,F> createDefaultSkin() {
        return new GridViewSkin<>(this);
    }

    @SuppressWarnings("unchecked")
    public GridViewSkin<T,F> getSkinn() {
        return (GridViewSkin) getSkin();
    }


    public ObservableList<T> getItemsRaw() {
        return itemsAll;
    }

    public ObservableList<T> getItemsShown() {
        return itemsSorted;
    }



    /**
     * Property for specifying how much spacing there is between each cell
     * in a row (i.e. how much horizontal spacing there is).
     */
    public final DoubleProperty horizontalCellSpacingProperty() {
        if (horizontalCellSpacing == null) {
            horizontalCellSpacing = new StyleableDoubleProperty(12) {
                @Override public CssMetaData<GridView<?,?>, Number> getCssMetaData() {
                    return GridView.StyleableProperties.HORIZONTAL_CELL_SPACING;
                }

                @Override public Object getBean() {
                    return GridView.this;
                }

                @Override public String getName() {
                    return "horizontalCellSpacing";
                }
            };
        }
        return horizontalCellSpacing;
    }
    private DoubleProperty horizontalCellSpacing;

    /**
     * Sets the amount of horizontal spacing there should be between cells in
     * the same row.
     * @param value The amount of spacing to use.
     */
    public final void setHorizontalCellSpacing(double value) {
        horizontalCellSpacingProperty().set(value);
    }

    /**
     * Returns the amount of horizontal spacing there is between cells in
     * the same row.
     */
    public final double getHorizontalCellSpacing() {
        return horizontalCellSpacing == null ? 12.0 : horizontalCellSpacing.get();
    }



    /**
     * Property for specifying how much spacing there is between each cell
     * in a column (i.e. how much vertical spacing there is).
     */
    private DoubleProperty verticalCellSpacing;
    public final DoubleProperty verticalCellSpacingProperty() {
        if (verticalCellSpacing == null) {
            verticalCellSpacing = new StyleableDoubleProperty(12) {
                @Override public CssMetaData<GridView<?,?>, Number> getCssMetaData() {
                    return GridView.StyleableProperties.VERTICAL_CELL_SPACING;
                }

                @Override public Object getBean() {
                    return GridView.this;
                }

                @Override public String getName() {
                    return "verticalCellSpacing";
                }
            };
        }
        return verticalCellSpacing;
    }

    /**
     * Sets the amount of vertical spacing there should be between cells in
     * the same column.
     * @param value The amount of spacing to use.
     */
    public final void setVerticalCellSpacing(double value) {
        verticalCellSpacingProperty().set(value);
    }

    /**
     * Returns the amount of vertical spacing there is between cells in
     * the same column.
     */
    public final double getVerticalCellSpacing() {
        return verticalCellSpacing == null ? 12.0 : verticalCellSpacing.get();
    }



    /**
     * Property representing the width that all cells should be.
     */
    public final DoubleProperty cellWidthProperty() {
        if (cellWidth == null) {
            cellWidth = new StyleableDoubleProperty(64) {
                @Override public CssMetaData<GridView<?,?>, Number> getCssMetaData() {
                    return GridView.StyleableProperties.CELL_WIDTH;
                }

                @Override public Object getBean() {
                    return GridView.this;
                }

                @Override public String getName() {
                    return "cellWidth";
                }
            };
        }
        return cellWidth;
    }
    private DoubleProperty cellWidth;

    /**
     * Sets the width that all cells should be.
     */
    public final void setCellWidth(double value) {
        cellWidthProperty().set(value);
    }

    /**
     * Returns the width that all cells should be.
     */
    public final double getCellWidth() {
        return cellWidth == null ? 64.0 : cellWidth.get();
    }


    /**
     * Property representing the height that all cells should be.
     */
    public final DoubleProperty cellHeightProperty() {
        if (cellHeight == null) {
            cellHeight = new StyleableDoubleProperty(64) {
                @Override public CssMetaData<GridView<?,?>, Number> getCssMetaData() {
                    return GridView.StyleableProperties.CELL_HEIGHT;
                }

                @Override public Object getBean() {
                    return GridView.this;
                }

                @Override public String getName() {
                    return "cellHeight";
                }
            };
        }
        return cellHeight;
    }
    private DoubleProperty cellHeight;

    /**
     * Sets the height that all cells should be.
     */
    public final void setCellHeight(double value) {
        cellHeightProperty().set(value);
    }

    /**
     * Returns the height that all cells should be.
     */
    public final double getCellHeight() {
        return cellHeight == null ? 64.0 : cellHeight.get();
    }


    private ObjectProperty<Callback<GridView<T,F>, GridCell<T,F>>> cellFactory;

    /**
     * Property representing the cell factory that is currently set in this
     * GridView, or null if no cell factory has been set (in which case the
     * default cell factory provided by the GridView skin will be used). The cell
     * factory is used for instantiating enough GridCell instances for the
     * visible area of the GridView. Refer to the GridView class documentation
     * for more information and examples.
     */
    public final ObjectProperty<Callback<GridView<T,F>, GridCell<T,F>>> cellFactoryProperty() {
        if (cellFactory == null) {
            cellFactory = new SimpleObjectProperty<>(this, "cellFactory");
        }
        return cellFactory;
    }

    /**
     * Sets the cell factory to use to create {@link GridCell} instances to
     * show in the GridView.
     */
    public final void setCellFactory(Callback<GridView<T,F>, GridCell<T,F>> value) {
        cellFactoryProperty().set(value);
    }

    /**
     * Returns the cell factory that will be used to create {@link GridCell}
     * instances to show in the GridView.
     */
    public final Callback<GridView<T,F>, GridCell<T,F>> getCellFactory() {
        return cellFactory == null ? null : cellFactory.get();
    }



    private static final String DEFAULT_STYLE_CLASS = "grid-view";

    private static class StyleableProperties {
        private static final CssMetaData<GridView<?,?>,Number> HORIZONTAL_CELL_SPACING =
            new CssMetaData<>("-fx-horizontal-cell-spacing", StyleConverter.getSizeConverter(), 12d) {

            @Override public Double getInitialValue(GridView<?,?> node) {
                return node.getHorizontalCellSpacing();
            }

            @Override public boolean isSettable(GridView<?,?> n) {
                return n.horizontalCellSpacing == null || !n.horizontalCellSpacing.isBound();
            }

            @Override
            @SuppressWarnings("unchecked")
            public StyleableProperty<Number> getStyleableProperty(GridView<?,?> n) {
                return (StyleableProperty<Number>)n.horizontalCellSpacingProperty();
            }
        };

        private static final CssMetaData<GridView<?,?>,Number> VERTICAL_CELL_SPACING =
            new CssMetaData<>("-fx-vertical-cell-spacing", StyleConverter.getSizeConverter(), 12d) {

            @Override public Double getInitialValue(GridView<?,?> node) {
                return node.getVerticalCellSpacing();
            }

            @Override public boolean isSettable(GridView<?,?> n) {
                return n.verticalCellSpacing == null || !n.verticalCellSpacing.isBound();
            }

            @Override
            @SuppressWarnings("unchecked")
            public StyleableProperty<Number> getStyleableProperty(GridView<?,?> n) {
                return (StyleableProperty<Number>)n.verticalCellSpacingProperty();
            }
        };

        private static final CssMetaData<GridView<?,?>,Number> CELL_WIDTH =
            new CssMetaData<>("-fx-cell-width", StyleConverter.getSizeConverter(), 64d) {

            @Override public Double getInitialValue(GridView<?,?> node) {
                return node.getCellWidth();
            }

            @Override public boolean isSettable(GridView<?,?> n) {
                return n.cellWidth == null || !n.cellWidth.isBound();
            }

            @Override
            @SuppressWarnings("unchecked")
            public StyleableProperty<Number> getStyleableProperty(GridView<?,?> n) {
                return (StyleableProperty<Number>)n.cellWidthProperty();
            }
        };

        private static final CssMetaData<GridView<?,?>,Number> CELL_HEIGHT =
            new CssMetaData<>("-fx-cell-height", StyleConverter.getSizeConverter(), 64d) {

            @Override public Double getInitialValue(GridView<?,?> node) {
                return node.getCellHeight();
            }

            @Override public boolean isSettable(GridView<?,?> n) {
                return n.cellHeight == null || !n.cellHeight.isBound();
            }

            @Override
            @SuppressWarnings("unchecked")
            public StyleableProperty<Number> getStyleableProperty(GridView<?,?> n) {
                return (StyleableProperty<Number>)n.cellHeightProperty();
            }
        };

        static final List<CssMetaData<? extends Styleable,?>> STYLEABLES = stream(Control.getClassCssMetaData())
                .append(HORIZONTAL_CELL_SPACING, VERTICAL_CELL_SPACING, CELL_WIDTH, CELL_HEIGHT)
                .collect(toCollection(() -> unmodifiableList(new ArrayList<>())));
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }
}
