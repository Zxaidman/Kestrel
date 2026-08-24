# AI Prompt: New Control Sizing and Positioning System

## 1. Goal
Create a layout system that uses the phone's actual screen size (like 2400x1080) for positioning buttons. Use 9 specific anchor points to keep everything standard and easy for new users.

## 2. Anchor Points and Rules
Let Max_X be the screen width and Max_Y be the screen height. 

### A. The 4 Corners (Use Positive Numbers Only)
Buttons placed here can only move inside the screen. The numbers for position (x, y) must always be positive.
* **Bottom-Left:** (0,0) is the corner. Moving right (+x) and up (+y) uses positive numbers.
* **Top-Left:** Corner is at the top left. Moving right (+x) and down (+y) uses positive numbers.
* **Bottom-Right:** Corner is at the bottom right. Moving left (+x) and up (+y) uses positive numbers.
* **Top-Right:** Corner is at the top right. Moving left (+x) and down (+y) uses positive numbers.

### B. Edges and Center (Can Use Negative and Positive Numbers)
Let Mid_X be half of the width and Mid_Y be half of the height (for example, 1200 and 540).
* **Center-Left & Center-Right:** The vertical position (y) can use negative or positive numbers between -Mid_Y and +Mid_Y.
* **Mid-Top & Mid-Bottom:** The horizontal position (x) can use negative or positive numbers between -Mid_X and +Mid_X.
* **Mid-Center:** Both x and y can use negative or positive numbers up to the half-screen limits.

## 3. Button Sizing
Buttons must have a minimum and maximum size limit. These limits change based on the type of gamepad layout being used so buttons never get too small to touch or too big for the screen.

## 4. Fixes and Improvements Needed
Make sure the system handles these two problems:
1. **Different Screen Sizes:** Do not lock the system to exactly 2400x1080. It must auto-scale to fit wide screens, tablets, and different phone sizes without stretching the buttons.
2. **Changing Anchors:** If a user changes a button's anchor (for example, from Bottom-Left to Top-Right), calculate the new numbers automatically so the button stays in the exact same spot on the screen without jumping.

## 5. Open for Criticise and improvement.
