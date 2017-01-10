package edu.mit.simile.longwell.command;

import javax.servlet.ServletException;

import edu.mit.simile.longwell.Message;

/**
 * Represents a type of Longwell command which can be invoked by an
 * HTTP message.
 */
public interface Command {

    /**
     * Execute this command, using the parameters in the given Message.
     */
    public void execute(Message msg) throws ServletException;
}
