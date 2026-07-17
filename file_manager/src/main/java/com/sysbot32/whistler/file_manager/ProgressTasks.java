package com.sysbot32.whistler.file_manager;

import lombok.experimental.UtilityClass;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

/**
 * Modal progress dialog + cancellable {@link SwingWorker} for long transfers.
 */
@UtilityClass
public final class ProgressTasks {
    public static <T> void run(
            final Component parent,
            final String title,
            final TransferControl control,
            final Callable<T> work,
            final Consumer<T> onSuccess,
            final Consumer<Exception> onError
    ) {
        Objects.requireNonNull(control, "control");
        Objects.requireNonNull(work, "work");

        final Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        final JDialog dialog = owner instanceof Frame
                ? new JDialog((Frame) owner, title, true)
                : new JDialog(owner, title, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setResizable(false);

        final JLabel message = new JLabel("Working…");
        message.setPreferredSize(new Dimension(360, 20));
        final JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setStringPainted(true);
        bar.setString("");
        final JButton cancel = new JButton("Cancel");

        final JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(cancel);
        final JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(message, BorderLayout.NORTH);
        center.add(bar, BorderLayout.CENTER);

        final JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 14, 8, 14));
        root.add(center, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);

        final SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                dialog.setVisible(false);
                dialog.dispose();
                try {
                    if (isCancelled() || control.isCancelled()) {
                        if (onError != null) {
                            onError.accept(new CancellationException("Cancelled"));
                        }
                        return;
                    }
                    final T result = get();
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                } catch (final Exception ex) {
                    final Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof CancellationException || control.isCancelled()) {
                        if (onError != null) {
                            onError.accept(new CancellationException("Cancelled"));
                        }
                        return;
                    }
                    if (onError != null) {
                        onError.accept(ex instanceof Exception e ? e : new Exception(cause));
                    }
                }
            }
        };

        cancel.addActionListener(e -> {
            control.cancel();
            worker.cancel(true);
            cancel.setEnabled(false);
            message.setText("Cancelling…");
        });

        final Timer timer = new Timer(80, e -> {
            final String detail = control.detail();
            if (detail != null && !detail.isBlank()) {
                message.setText(detail);
            }
            final int pct = control.percent();
            if (pct >= 0) {
                bar.setIndeterminate(false);
                bar.setMaximum(100);
                bar.setValue(pct);
                bar.setString(pct + "%");
            } else {
                bar.setIndeterminate(true);
                bar.setString("");
            }
        });
        timer.start();

        worker.execute();
        dialog.setVisible(true);
        timer.stop();
    }
}
