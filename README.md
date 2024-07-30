# Gui-Prototyper
An application used for creating interactive app prototypes from drawn sketches / image files. 
Intended to be used with the [reMarkable](https://remarkable.com/), but works with other tools also.

This tool offers two main modes / features:
1. **Editing:** For building a visual prototype
2. **Presenting:** For showing the prototype to others / interacting with it

## Usage Instructions
Here are quick instructions on how to use this application.

### Starting the application
In the terminal / command line, go to the directory where the Gui-Prototyper.jar resides. 
Then, call `java-jar Gui-Prototyper.jar`.

On Windows, you should be able to just double-click the Gui-Prototyper.jar file.

### Creating a new project
In order to create a new project, you will need some image files that represent you application.

Here's how to start a new project in a few quick steps:
1. Click the big `(+)` button to create a new project.
2. In the window that opens, **drag** your sketch / image files to the white area.
3. Give a name to your project by typing it into the `Project Name` -field
4. Select `Start`

Next, you will be moved to the editing view.

### Editing a project
In order to open a project in the editing view, in the main window, click the **pen icon** next to your project's name.  
In the view that opens, you can edit the project's name and add more files to it, if needed.  
Once you're ready, select Start to go to editing view.

Once you're in editing mode, there are the following options available to you:
- **Creating sub-views**
  - By d**ragging with the left mouse button**, you can select a region of the current image and create a new view out of it
- **Creating and interacting with links**
  - By **dragging with the right mouse button**, you can create a link (which appears as a blue region)
  - The **link edit panel** opens when you create a link or when you **click** it with the left mouse button
  - In the link editing view, you can:
    - **Edit** the view where the link points to by using the `Link target` field
    - **Delete** the link by clicking the **trash can icon**
    - **Close** the link view by clicking the **up-arrow**
  - By **clicking a link** with the left mouse button **while holding the Ctrl key**, 
    you can **follow** that link to its target view
- You can **change the currently viewed image** with the **arrow buttons** at the top left and the top right corner
- You can **rename** the current view by editing the `View Name` field
- You can make the current view the initially displayed view by clicking the `(1)` button
  - When the current view is the first view, the button shows as blue
- You can **delete** a view by clicking the **trash can icon**
- You can always **add** new views by **dragging image files to the editing window**

Once you're done, just close the window from the top right corner. 
The changes you make are **automatically saved**.

### Presenting a project
In order to enter the presentation mode, click the play `[>]` icon next to you project's name in the main window.

Once in presentation mode, you have the following options available to you:
- **Navigating** between views
  - You can **select your current view** by using the **drop-down menu** at the top of the window
  - Whenever you're located in a sub-view, you can **move to the parent view** by clicking the **up-arrow**
  - You may also **cycle** between the views by selecting the **left** or the **right arrow** in the top panel
  - Whenever you **click a link** area (which appears in blue*), the linked view will open
- **Drawing**
  - By **dragging with the left mouse button**, you can draw an orange line

(*) It takes some time for the application to render the link areas. 
However, they are clickable even before they appear blue.