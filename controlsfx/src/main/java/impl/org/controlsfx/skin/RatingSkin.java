/*
  Copyright (c) 2013, 2016 ControlsFX
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:
      * Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.
      * Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.
      * Neither the name of ControlsFX, any associated website, nor the
  names of its contributors may be used to endorse or promote products
  derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package impl.org.controlsfx.skin;

import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.controlsfx.control.Rating;
import org.controlsfx.tools.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 */
public class RatingSkin extends SkinBase<Rating> {

    /***************************************************************************
     *
     * Private fields
     *
     **************************************************************************/

    private static final String STRONG = "strong"; //$NON-NLS-1$

    private boolean updateOnHover;

    // the container for the traditional rating control. If updateOnHover , this will show a combination of strong
    // and non-strong graphics, depending on the current rating value
    private Pane backgroundContainer;

    private int rating = -1;

    private void updateRatingFromMouseEvent(MouseEvent event) {
        Rating control = getSkinnable();
        if (!control.ratingProperty().isBound()) {
            Point2D mouseLocation = new Point2D(event.getSceneX(), event.getSceneY());
            control.setRating(calculateRating(mouseLocation));
        }
    }

    private EventHandler<MouseEvent> eventHandler;

    /***************************************************************************
     *
     * Constructors
     *
     **************************************************************************/

    public RatingSkin(Rating control) {
        super(control);

        eventHandler = event -> {
            if (event.getSource() instanceof Region)
                updateRatingFromMouseEvent(event);
        };

        this.updateOnHover = control.isUpdateOnHover();

        // init
        recreateAll();
        updateRating();
        // -- end init

        registerChangeListener(control.ratingProperty(), e -> updateRating());
        registerChangeListener(control.maxProperty(), e -> recreateAll());
        registerChangeListener(control.orientationProperty(), e -> recreateAll());
        registerChangeListener(control.updateOnHoverProperty(), e -> {
            this.updateOnHover = getSkinnable().isUpdateOnHover();
            updateEventHandler();
        });
    }


    /***************************************************************************
     *
     * Implementation
     *
     **************************************************************************/

    private void createContainer() {

        backgroundContainer = isVertical() ? new VBox() : new HBox();
        backgroundContainer.getStyleClass().add("container"); //$NON-NLS-1$
        getChildren().setAll(backgroundContainer);
    }

    private void recreateAll() {
        recreateButtons();
        updateEventHandler();
    }


    private void recreateButtons() {
        createContainer();

        final Rating control = getSkinnable();

        List<Node> nodes = IntStream.rangeClosed(1, control.getMax())
                .mapToObj(i -> createBackButton()).collect(Collectors.toList());

        backgroundContainer.getChildren().setAll(nodes);

        updateRating();
    }

    private void updateEventHandler() {
        backgroundContainer.removeEventHandler(MouseEvent.MOUSE_MOVED, eventHandler);
        backgroundContainer.removeEventHandler(MouseEvent.MOUSE_CLICKED, eventHandler);

        if (updateOnHover) {
            // if we support updateOnHover, calculate the intended rating based on the mouse
            // location and update the control property with it.
            backgroundContainer.addEventHandler(MouseEvent.MOUSE_MOVED, eventHandler);
        } else {
            // if we are not updating on hover, calculate the intended rating based on the mouse
            // location and update the control property with it.
            backgroundContainer.addEventHandler(MouseEvent.MOUSE_CLICKED, eventHandler);
        }

    }

    // Calculate the rating based on a mouse position (in Scene coordinates).
    // If we support partial ratings, the value is calculated directly.
    // Otherwise the ceil of the value is computed.
    private int calculateRating(Point2D sceneLocation) {
        final Point2D b = backgroundContainer.sceneToLocal(sceneLocation);

        final double x = b.getX();
        final double y = b.getY();

        final Rating control = getSkinnable();

        final int max = control.getMax();
        final double w = control.getWidth() - (snappedLeftInset() + snappedRightInset());
        final double h = control.getHeight() - (snappedTopInset() + snappedBottomInset());

        double newRating;

        if (isVertical()) {
            newRating = ((h - y) / h) * max;
        } else {
            newRating = (x / w) * max;
        }

        return Utils.clamp(0, (int) Math.ceil(newRating), control.getMax());
    }

//    private double getSpacing() {
//        return (backgroundContainer instanceof HBox) ?
//                ((HBox)backgroundContainer).getSpacing() :
//                ((VBox)backgroundContainer).getSpacing();
//    }

    private Node createBackButton() {
        Node btn = new Region();
        btn.getStyleClass().add("button"); //$NON-NLS-1$
        return btn;
    }

    // Update the skin based on a new value for the rating.
    // updates the style classes for the buttons.

    private void updateRating() {

        int newRating = getSkinnable().getRating();

        rating = Utils.clamp(0, newRating, getSkinnable().getMax());

        updateButtonStyles();
    }

    private void updateButtonStyles() {
        final int max = getSkinnable().getMax();

        // make a copy of the buttons list so that we can reverse the order if
        // the list is vertical (as the buttons are ordered bottom to top).
        final List<Node> buttons = new ArrayList<>(backgroundContainer.getChildren());
        if (isVertical()) {
            Collections.reverse(buttons);
        }

        IntStream.range(0, max).forEach(i -> {
            Node button = buttons.get(i);

            final List<String> styleClass = button.getStyleClass();
            final boolean containsStrong = styleClass.contains(STRONG);
            if (i < rating) {
                if (!containsStrong) {
                    styleClass.add(STRONG);
                }
            } else if (containsStrong) {
                styleClass.remove(STRONG);
            }
        });
    }

    private boolean isVertical() {
        return getSkinnable().getOrientation() == Orientation.VERTICAL;
    }

    @Override
    protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
    }
}
