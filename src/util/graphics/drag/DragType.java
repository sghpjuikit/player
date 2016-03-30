/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.graphics.drag;

import javafx.scene.input.TransferMode;

/**
 * Complete enumeration for {@link TransferMode}.
 *
 * @author Martin Polakovic
 */
public enum DragType {
    COPY{
        @Override public TransferMode[] val() { 
            return new TransferMode[] {TransferMode.COPY};
        }
    },
    LINK{
        @Override public TransferMode[] val() {
            return new TransferMode[] {TransferMode.LINK};
        }
    },
    MOVE{
        @Override public TransferMode[] val() {
            return new TransferMode[] {TransferMode.MOVE};
        }
    },
    ANY{
        @Override public TransferMode[] val() {
            return TransferMode.ANY;
        }
    },
    COPYorMOVE{
        @Override public TransferMode[] val() {
            return TransferMode.COPY_OR_MOVE;
        }
    },
    NONE{
        @Override public TransferMode[] val() {
            return TransferMode.NONE;
        }
    };

    /** @return array of {@link TransferMode}.*/
    public abstract TransferMode[] val();
}