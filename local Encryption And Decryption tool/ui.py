import customtkinter as ctk
from tkinter import filedialog
import os

from enc import run_encryption
from dec import run_decryption

# Set global appearance and theme
ctk.set_appearance_mode("Dark")
ctk.set_default_color_theme("blue")

class DropZone(ctk.CTkFrame):
    """A custom widget representing a drag-and-drop zone."""
    def __init__(self, master, text, command=None, **kwargs):
        super().__init__(master, corner_radius=20, border_width=2, border_color="#555555", fg_color="#2b2b2b", **kwargs)
        
        self.command = command
        self.file_path = None
        
        # Inner label
        self.label = ctk.CTkLabel(self, text=text, font=("Arial", 16), text_color="#aaaaaa", wraplength=150)
        self.label.place(relx=0.5, rely=0.5, anchor="center")
        
        # Bind events to simulate highlight and click-to-browse
        self.bind("<Enter>", self.on_enter)
        self.bind("<Leave>", self.on_leave)
        self.bind("<Button-1>", self.on_click)
        self.label.bind("<Enter>", self.on_enter)
        self.label.bind("<Leave>", self.on_leave)
        self.label.bind("<Button-1>", self.on_click)

    def on_enter(self, event):
        """Highlight the box when hovering (simulating drag over)"""
        self.configure(border_color="#3a7ebf", fg_color="#333333")
        self.label.configure(text_color="#ffffff")

    def on_leave(self, event):
        """Revert highlight when leaving"""
        if not self.file_path:
            self.configure(border_color="#555555", fg_color="#2b2b2b")
            self.label.configure(text_color="#aaaaaa")

    def on_click(self, event):
        """Fallback: Open file dialog on click since OS drag-and-drop requires extra libs"""
        path = filedialog.askopenfilename()
        if path:
            self.file_path = path
            filename = os.path.basename(path)
            self.label.configure(text=f"Selected:\n{filename}", text_color="#00FF00")
            self.configure(border_color="#00FF00")
            if self.command:
                self.command(path)


class CryptoApp(ctk.CTk):
    def __init__(self):
        super().__init__()

        # 1. Window setup (Dark mode, rounded corners handled by CTk)
        self.title("Encrypt / Decrypt Tool")
        self.geometry("800x550")
        self.minsize(700, 500)
        
        # Layout configuration
        self.grid_rowconfigure(1, weight=1)
        self.grid_columnconfigure(0, weight=1)

        # 2. Top Section: Mode Toggle
        self.top_frame = ctk.CTkFrame(self, fg_color="transparent")
        self.top_frame.grid(row=0, column=0, pady=(30, 10), sticky="ew")
        
        self.mode_var = ctk.StringVar(value="Encrypt")
        self.mode_switch = ctk.CTkSegmentedButton(
            self.top_frame, 
            values=["Encrypt", "Decrypt"], 
            variable=self.mode_var,
            command=self.switch_mode,
            width=300,
            height=40,
            font=("Arial", 16, "bold")
        )
        self.mode_switch.pack(pady=10)

        # 3. Middle Section: Drop Zones Container
        self.middle_frame = ctk.CTkFrame(self, corner_radius=15)
        self.middle_frame.grid(row=1, column=0, padx=40, pady=10, sticky="nsew")
        
        # Create the drop zones (placed dynamically later)
        self.file_zone = DropZone(self.middle_frame, text="Drag And Drop your\nfile here\n(or click to browse)", width=250, height=250)
        self.key_zone = DropZone(self.middle_frame, text="Drag and drop the\nkey here\n(or click to browse)", width=250, height=250)

        # 4. Bottom Section: Action and Status
        self.bottom_frame = ctk.CTkFrame(self, fg_color="transparent")
        self.bottom_frame.grid(row=2, column=0, pady=(10, 30), sticky="ew")
        
        self.action_btn = ctk.CTkButton(
            self.bottom_frame, 
            text="Proceed with Encryption", 
            command=self.execute_process,
            height=45,
            width=200,
            font=("Arial", 15, "bold"),
            corner_radius=8
        )
        self.action_btn.pack(pady=(0, 15))

        self.status_label = ctk.CTkLabel(
            self.bottom_frame, 
            text="Status: Waiting for input...", 
            text_color="gray",
            font=("Arial", 14)
        )
        self.status_label.pack()

        # Initialize default view
        self.switch_mode("Encrypt")


    def switch_mode(self, selected_mode):
        """Switches the UI layout based on Encrypt or Decrypt mode."""
        self.status_label.configure(text="Status: Waiting for input...", text_color="gray")
        
        # Reset file selections visually
        self.file_zone.file_path = None
        self.file_zone.label.configure(text="Drag And Drop your\nfile here\n(or click to browse)", text_color="#aaaaaa")
        self.file_zone.configure(border_color="#555555")
        
        self.key_zone.file_path = None
        self.key_zone.label.configure(text="Drag and drop the\nkey here\n(or click to browse)", text_color="#aaaaaa")
        self.key_zone.configure(border_color="#555555")

        if selected_mode == "Encrypt":
            # Show only 1 box, centered
            self.key_zone.place_forget()
            self.file_zone.place(relx=0.5, rely=0.5, anchor="center")
            self.action_btn.configure(text="Proceed with Encryption")
            
        elif selected_mode == "Decrypt":
            # Show 2 boxes side-by-side
            self.file_zone.place(relx=0.3, rely=0.5, anchor="center")
            self.key_zone.place(relx=0.7, rely=0.5, anchor="center")
            self.action_btn.configure(text="Proceed with Decryption")


    def execute_process(self):
        """Action triggered when the bottom button is pressed."""
        mode = self.mode_var.get()
        
        if mode == "Encrypt":
            if not self.file_zone.file_path:
                self.status_label.configure(text="Error: Please select a file to encrypt!", text_color="#FF5555")
                return

            save_dir = filedialog.askdirectory(title="Select Folder to Save Encrypted File & Key")
            if not save_dir:
                self.status_label.configure(text="Status: Cancelled", text_color="gray")
                return

            try:
                # Calls your logic from enc.py
                enc_path, key_path = run_encryption(self.file_zone.file_path, save_dir)
                self.status_label.configure(
                    text=f"Success! Saved encrypted file and key in folder:\n{save_dir}",
                    text_color="#55FF55"
                )
            except Exception as e:
                self.status_label.configure(text=f"Encryption Failed: {str(e)}", text_color="#FF5555")

        elif mode == "Decrypt":
            if not self.file_zone.file_path or not self.key_zone.file_path:
                self.status_label.configure(text="Error: Please select BOTH the encrypted file and key file!", text_color="#FF5555")
                return

            save_dir = filedialog.askdirectory(title="Select Folder to Save Decrypted File")
            if not save_dir:
                self.status_label.configure(text="Status: Cancelled", text_color="gray")
                return

            try:
                # Calls your logic from dec.py
                dec_path = run_decryption(self.file_zone.file_path, self.key_zone.file_path, save_dir)
                self.status_label.configure(
                    text=f"Success! Decrypted file saved to:\n{dec_path}",
                    text_color="#55FF55"
                )
            except Exception as e:
                self.status_label.configure(text=f"Decryption Failed: {str(e)}", text_color="#FF5555")
                

if __name__ == "__main__":
    app = CryptoApp()
    app.mainloop()